package com.example.demo.dtos;

public record AccountSearchResponse(
        String iban,
        String firstName,
        String lastName
) {}
