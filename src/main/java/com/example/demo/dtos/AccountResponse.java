package com.example.demo.dtos;

import com.example.demo.entities.enums.AccountStatus;
import com.example.demo.entities.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        int id,
        int userId,
        String iban,
        AccountType type,
        BigDecimal balance,
        BigDecimal absoluteTransferLimit,
        BigDecimal dailyTransferLimit,
        AccountStatus status,
        LocalDateTime createdAt
) {}
