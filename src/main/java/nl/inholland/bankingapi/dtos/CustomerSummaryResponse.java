package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;

import java.time.LocalDateTime;

public record CustomerSummaryResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        CustomerStatus status,
        LocalDateTime createdAt
) {}
