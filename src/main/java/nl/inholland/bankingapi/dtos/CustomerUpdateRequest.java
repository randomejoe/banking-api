package nl.inholland.bankingapi.dtos;

public record CustomerUpdateRequest(
        String status,
        String firstName,
        String lastName,
        String phoneNumber
) {}
