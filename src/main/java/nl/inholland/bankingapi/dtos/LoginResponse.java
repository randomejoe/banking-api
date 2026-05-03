package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        int expiresIn,
        UserRole role,
        CustomerStatus status
) {}
