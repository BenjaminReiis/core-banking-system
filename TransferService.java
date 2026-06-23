package com.example.corebanking.service;

import com.example.corebanking.domain.Account;
import com.example.corebanking.domain.EntryType;
import com.example.corebanking.domain.LedgerEntry;
import com.example.corebanking.domain.Transfer;
import com.example.corebanking.domain.TransferStatus;
import com.example.corebanking.dto.TransferRequest;
import com.example.corebanking.dto.TransferResponse;
import com.example.corebanking.exception.BusinessException;
import com.example.corebanking.exception.InsufficientFundsException;
import com.example.corebanking.outbox.OutboxEvent;
import com.example.corebanking.outbox.OutboxRepository;
import com.example.corebanking.repository.AccountRepository;
import com.example.corebanking.repository.LedgerEntryRepository;
import com.example.corebanking.repository.TransferRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final OutboxRepository outboxRepository;

    public TransferService(AccountRepository accountRepository,
                           TransferRepository transferRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           OutboxRepository outboxRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request, String currentUserId) {
        transferRepository.findByIdempotencyKey(request.getIdempotencyKey()).ifPresent(existing -> {
            throw new BusinessException("Transferencia duplicada para a mesma idempotency key");
        });

        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new BusinessException("Conta de origem e destino nao podem ser iguais");
        }

        UUID firstLock = request.getSourceAccountId();
        UUID secondLock = request.getDestinationAccountId();

        if (firstLock.compareTo(secondLock) > 0) {
            UUID temp = firstLock;
            firstLock = secondLock;
            secondLock = temp;
        }

        Account firstAccount = accountRepository.findByIdForUpdate(firstLock)
                .orElseThrow(() -> new BusinessException("Conta nao encontrada: " + firstLock));

        Account secondAccount = accountRepository.findByIdForUpdate(secondLock)
                .orElseThrow(() -> new BusinessException("Conta nao encontrada: " + secondLock));

        Account sourceAccount = firstAccount.getId().equals(request.getSourceAccountId()) ? firstAccount : secondAccount;
        Account destinationAccount = secondAccount.getId().equals(request.getDestinationAccountId()) ? secondAccount : firstAccount;

        if (!sourceAccount.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException("Usuario nao autorizado para movimentar a conta de origem");
        }

        if (!sourceAccount.getCurrency().equalsIgnoreCase(request.getCurrency())
                || !destinationAccount.getCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new BusinessException("Moeda incompativel com as contas");
        }

        if (sourceAccount.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente");
        }

        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(request.getAmount()));
        destinationAccount.setAvailableBalance(destinationAccount.getAvailableBalance().add(request.getAmount()));
        sourceAccount.setUpdatedAt(LocalDateTime.now());
        destinationAccount.setUpdatedAt(LocalDateTime.now());

        Transfer transfer = new Transfer();
        transfer.setId(UUID.randomUUID());
        transfer.setIdempotencyKey(request.getIdempotencyKey());
        transfer.setSourceAccountId(sourceAccount.getId());
        transfer.setDestinationAccountId(destinationAccount.getId());
        transfer.setAmount(request.getAmount());
        transfer.setCurrency(request.getCurrency().toUpperCase());
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setRequestedBy(currentUserId);
        transfer.setCreatedAt(LocalDateTime.now());
        transfer.setCompletedAt(LocalDateTime.now());

        LedgerEntry debit = new LedgerEntry();
        debit.setId(UUID.randomUUID());
        debit.setTransferId(transfer.getId());
        debit.setAccountId(sourceAccount.getId());
        debit.setEntryType(EntryType.DEBIT);
        debit.setAmount(request.getAmount());
        debit.setCurrency(request.getCurrency().toUpperCase());
        debit.setCreatedAt(LocalDateTime.now());

        LedgerEntry credit = new LedgerEntry();
        credit.setId(UUID.randomUUID());
        credit.setTransferId(transfer.getId());
        credit.setAccountId(destinationAccount.getId());
        credit.setEntryType(EntryType.CREDIT);
        credit.setAmount(request.getAmount());
        credit.setCurrency(request.getCurrency().toUpperCase());
        credit.setCreatedAt(LocalDateTime.now());

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
        transferRepository.save(transfer);
        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("TRANSFER");
        event.setAggregateId(transfer.getId().toString());
        event.setEventType("TRANSFER_COMPLETED");
        event.setPayload("{\"transferId\":\"" + transfer.getId() + "\",\"amount\":\"" + transfer.getAmount() + "\"}");
        event.setStatus("PENDING");
        event.setCreatedAt(LocalDateTime.now());
        outboxRepository.save(event);

        TransferResponse response = new TransferResponse();
        response.setTransferId(transfer.getId());
        response.setStatus(transfer.getStatus().name());
        response.setSourceAccountId(sourceAccount.getId());
        response.setDestinationAccountId(destinationAccount.getId());
        response.setAmount(transfer.getAmount());
        response.setCurrency(transfer.getCurrency());
        response.setCompletedAt(transfer.getCompletedAt());

        return response;
    }
}
