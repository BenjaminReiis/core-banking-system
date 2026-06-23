package com.example.corebanking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransferResponse {

    private UUID transferId;
    private String status;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime completedAt;

    public UUID getTransferId() {
        return transferId;
    }

    public void setTransferId(UUID transferId) {
        this.transferId = transferId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(UUID destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
