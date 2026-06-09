package nl.inholland.bankingapi.dtos;

public record TransferTargetResponse(
        String iban,
        String firstName,
        String lastName
) {}
