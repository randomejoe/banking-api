package com.example.demo.services;

import com.example.demo.models.TransactionModel;
import com.example.demo.models.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private List<TransactionModel> transactions = new ArrayList<>();
    private int currentId = 0;

    public List<TransactionModel> getAll(String iban, TransactionType type, BigDecimal minAmount, BigDecimal maxAmount) {
        return transactions.stream()
                .filter(t -> iban == null || iban.equals(t.getFromIban()) || iban.equals(t.getToIban()))
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> minAmount == null || t.getAmount().compareTo(minAmount) >= 0)
                .filter(t -> maxAmount == null || t.getAmount().compareTo(maxAmount) <= 0)
                .collect(Collectors.toList());
    }

    public List<TransactionModel> getByIbans(List<String> ibans) {
        return transactions.stream()
                .filter(t -> ibans.contains(t.getFromIban()) || ibans.contains(t.getToIban()))
                .collect(Collectors.toList());
    }

    public TransactionModel create(String fromIban, String toIban, int initiatedByUserId, BigDecimal amount, TransactionType type, String description) {
        currentId++;
        TransactionModel transaction = new TransactionModel(currentId, fromIban, toIban, initiatedByUserId, amount, type, description, LocalDateTime.now());
        transactions.add(transaction);
        return transaction;
    }
}