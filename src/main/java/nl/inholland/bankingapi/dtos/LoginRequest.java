package nl.inholland.bankingapi.dtos;

public record LoginRequest(
        String email,
        String password
) {}
