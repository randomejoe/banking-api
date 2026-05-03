package com.example.demo.dtos;

import com.example.demo.entities.enums.CustomerStatus;

public record CustomerProfileResponse(
        int id,
        int userId,
        String bsn,
        String phoneNumber,
        CustomerStatus status
) {}
