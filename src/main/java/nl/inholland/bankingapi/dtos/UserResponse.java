package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        LocalDateTime createdAt
) {}
