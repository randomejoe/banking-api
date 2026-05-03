package com.example.demo.dtos;

import com.example.demo.entities.enums.AccountStatus;
import com.example.demo.entities.enums.AccountType;

import java.math.BigDecimal;

public record AccountSummaryResponse(
        String iban,
        AccountType type,
        BigDecimal balance,
        AccountStatus status
) {}
