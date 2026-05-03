package com.example.demo.dtos;

import java.math.BigDecimal;

public record AccountLimitUpdateRequest(
        BigDecimal absoluteTransferLimit,
        BigDecimal dailyTransferLimit
) {}
