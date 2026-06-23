package com.example.corebanking.controller;

import com.example.corebanking.dto.TransferRequest;
import com.example.corebanking.dto.TransferResponse;
import com.example.corebanking.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request,
                                     Authentication authentication) {
        String currentUserId = authentication.getName();
        return transferService.transfer(request, currentUserId);
    }
}
