package nl.inholland.bankingapi.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import nl.inholland.bankingapi.entities.enums.AccountStatus;

import java.math.BigDecimal;

public record AccountUpdateRequest(
        @PositiveOrZero(message = "Absolute transfer limit must be zero or greater")
        BigDecimal absoluteTransferLimit,

        @PositiveOrZero(message = "Daily transfer limit must be zero or greater")
        BigDecimal dailyTransferLimit,

        AccountStatus status
) {
    @AssertTrue(message = "At least one field must be provided")
    public boolean hasAtLeastOneField() {
        return absoluteTransferLimit != null || dailyTransferLimit != null || status != null;
    }
}
