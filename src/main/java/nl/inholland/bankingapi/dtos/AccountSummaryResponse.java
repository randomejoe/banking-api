package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;

import java.math.BigDecimal;

public record AccountSummaryResponse(
        String iban,
        AccountType type,
        BigDecimal balance,
        AccountStatus status
) {}
