package nl.inholland.bankingapi.dtos;

public record TokenResponse(
        String value,
        long expiration,
        String type
) {}
