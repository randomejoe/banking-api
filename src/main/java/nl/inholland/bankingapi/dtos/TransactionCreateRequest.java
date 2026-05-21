package nl.inholland.bankingapi.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import nl.inholland.bankingapi.entities.enums.TransactionType;

import java.math.BigDecimal;

public record TransactionCreateRequest(
        String fromIban,
        String toIban,

        Integer initiatedByUserId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description
) {}
