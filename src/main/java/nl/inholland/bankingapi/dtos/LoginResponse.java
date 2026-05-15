package nl.inholland.bankingapi.dtos;

public record LoginResponse(
        TokenResponse token,
        CurrentUserResponse user
) {}
