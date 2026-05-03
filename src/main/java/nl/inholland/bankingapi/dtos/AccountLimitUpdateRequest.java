package nl.inholland.bankingapi.dtos;

import java.math.BigDecimal;

public record AccountLimitUpdateRequest(
        BigDecimal absoluteTransferLimit,
        BigDecimal dailyTransferLimit
) {}
