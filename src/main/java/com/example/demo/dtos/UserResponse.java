package com.example.demo.dtos;

import com.example.demo.entities.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        LocalDateTime createdAt
) {}
