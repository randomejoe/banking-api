package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.CustomerStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerDetailResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        String bsn,
        String phoneNumber,
        CustomerStatus status,
        LocalDateTime createdAt,
        BigDecimal totalBalance,
        List<AccountSummaryResponse> accounts
) {}
