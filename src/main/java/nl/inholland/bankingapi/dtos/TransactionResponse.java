package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        int id,
        String fromIban,
        String toIban,
        int initiatedByUserId,
        BigDecimal amount,
        TransactionType type,
        String description,
        LocalDateTime timestamp
) {}
