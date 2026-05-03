package com.example.demo.dtos;

import java.math.BigDecimal;

public record TransactionCreateRequest(
        String fromIban,
        String toIban,
        int initiatedByUserId,
        BigDecimal amount,
        String type,
        String description
) {}
