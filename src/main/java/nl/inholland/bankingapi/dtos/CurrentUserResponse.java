package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;

public record CurrentUserResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        CustomerStatus status,
        String bsn,
        String phoneNumber
) {}
