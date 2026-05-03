package com.example.demo.controllers;

import com.example.demo.dtos.TransactionCreateRequest;
import com.example.demo.dtos.TransactionResponse;
import com.example.demo.entities.Transaction;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.TransactionType;
import com.example.demo.mappers.TransactionMapper;
import com.example.demo.services.AuthService;
import com.example.demo.services.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("transactions")
public class TransactionController {

    final private TransactionService transactionService;
    final private AuthService authService;
    final private TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, AuthService authService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.authService = authService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("")
    List<TransactionResponse> getAll(@RequestParam(required = false) String iban,
                                     @RequestParam(required = false) TransactionType type,
                                     @RequestParam(required = false) BigDecimal minAmount,
                                     @RequestParam(required = false) BigDecimal maxAmount) {
        return transactionService.getAll(iban, type, minAmount, maxAmount).stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    @PostMapping("")
    TransactionResponse create(@RequestBody TransactionCreateRequest request) {
        User initiatedBy = authService.getUserById(request.initiatedByUserId());
        TransactionType type = TransactionType.valueOf(request.type());
        Transaction transaction = transactionService.create(request.fromIban(), request.toIban(), initiatedBy, request.amount(), type, request.description());
        return transactionMapper.toResponse(transaction);
    }
}
