package nl.inholland.bankingapi.dtos;

public record CustomerUpdateRequest(
        String status,
        String firstName,
        String lastName,
        String phoneNumber,
        java.math.BigDecimal absoluteTransferLimit,
        java.math.BigDecimal dailyTransferLimit
) {}
