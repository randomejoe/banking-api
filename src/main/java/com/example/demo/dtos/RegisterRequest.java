package com.example.demo.dtos;

public record RegisterRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String bsn,
        String phoneNumber
) {}
