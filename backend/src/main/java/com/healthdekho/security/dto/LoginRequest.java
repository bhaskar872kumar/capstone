package com.healthdekho.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Enter your email.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Enter your password.")
        @Size(max = 200)
        String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
