package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.CustomerStatus;

public record CustomerProfileResponse(
        int id,
        int userId,
        String bsn,
        String phoneNumber,
        CustomerStatus status
) {}
