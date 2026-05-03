package com.example.demo.dtos;

import com.example.demo.entities.enums.CustomerStatus;
import com.example.demo.entities.enums.UserRole;

import java.time.LocalDateTime;

public record CustomerSummaryResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        CustomerStatus status,
        LocalDateTime createdAt
) {}
