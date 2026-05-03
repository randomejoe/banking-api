package nl.inholland.bankingapi.dtos;

public record OwnerSummaryResponse(
        int id,
        String firstName,
        String lastName
) {}
