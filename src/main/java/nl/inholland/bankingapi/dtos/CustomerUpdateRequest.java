package nl.inholland.bankingapi.dtos;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustomerUpdateRequest(
        String status,

        @Size(max = 64, message = "First name must be at most 64 characters")
        String firstName,

        @Size(max = 64, message = "Last name must be at most 64 characters")
        String lastName,

        @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Phone number must be between 10 and 15 digits")
        String phoneNumber,

        @PositiveOrZero(message = "Absolute transfer limit must be zero or greater")
        BigDecimal absoluteTransferLimit,

        @PositiveOrZero(message = "Daily transfer limit must be zero or greater")
        BigDecimal dailyTransferLimit
) {}
