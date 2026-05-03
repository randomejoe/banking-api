package com.example.demo.mappers;

import com.example.demo.dtos.TransactionResponse;
import com.example.demo.entities.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) return null;
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromIban(),
                transaction.getToIban(),
                transaction.getInitiatedByUserId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getTimestamp()
        );
    }
}
