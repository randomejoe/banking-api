package com.example.demo.dtos;

import com.example.demo.entities.enums.CustomerStatus;
import com.example.demo.entities.enums.UserRole;

public record CurrentUserResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        CustomerStatus status,
        String bsn,
        String phoneNumber
) {}
