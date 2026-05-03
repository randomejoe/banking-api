package com.example.demo.dtos;

import com.example.demo.entities.enums.CustomerStatus;
import com.example.demo.entities.enums.UserRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        int expiresIn,
        UserRole role,
        CustomerStatus status
) {}
