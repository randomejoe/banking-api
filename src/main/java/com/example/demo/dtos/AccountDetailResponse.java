package com.example.demo.dtos;

import com.example.demo.entities.enums.AccountStatus;
import com.example.demo.entities.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountDetailResponse(
        String iban,
        AccountType type,
        BigDecimal balance,
        AccountStatus status,
        BigDecimal absoluteTransferLimit,
        BigDecimal dailyTransferLimit,
        LocalDateTime createdAt,
        OwnerSummaryResponse owner
) {}
