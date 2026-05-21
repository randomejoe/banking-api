package nl.inholland.bankingapi.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[^a-zA-Z0-9]).{8,}$",
                message = "Password must be at least 8 characters and contain at least one uppercase letter, one lowercase letter, and one special character"
        )
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 64, message = "First name must be at most 64 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 64, message = "Last name must be at most 64 characters")
        String lastName,

        @NotBlank(message = "BSN is required")
        @Pattern(regexp = "\\d{8,9}", message = "BSN must be 8 or 9 digits")
        String bsn,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^06[0-9]{8}$", message = "Phone number must be a valid.")
        String phoneNumber
) {}
