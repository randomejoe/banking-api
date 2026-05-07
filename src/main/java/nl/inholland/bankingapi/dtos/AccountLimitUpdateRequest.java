package nl.inholland.bankingapi.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AccountLimitUpdateRequest(
        @NotNull(message = "Absolute transfer limit is required")
        @PositiveOrZero(message = "Absolute transfer limit must be zero or greater")
        BigDecimal absoluteTransferLimit,

        @NotNull(message = "Daily transfer limit is required")
        @PositiveOrZero(message = "Daily transfer limit must be zero or greater")
        BigDecimal dailyTransferLimit
) {}
