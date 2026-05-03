package nl.inholland.bankingapi.dtos;

public record AccountSearchResponse(
        String iban,
        String firstName,
        String lastName
) {}
