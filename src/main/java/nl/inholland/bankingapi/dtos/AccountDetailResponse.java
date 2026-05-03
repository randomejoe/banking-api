package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;

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
