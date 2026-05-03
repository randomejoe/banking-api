package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;

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
