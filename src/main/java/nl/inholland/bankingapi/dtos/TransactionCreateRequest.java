package nl.inholland.bankingapi.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionCreateRequest(
        String fromIban,
        String toIban,

        @Positive(message = "Initiated by user ID must be a positive number")
        int initiatedByUserId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Transaction type is required")
        String type,

        String description
) {}
