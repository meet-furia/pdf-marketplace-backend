package com.meet.pdf_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record Auth0RegisterRequestDTO(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        String email,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Auth0 user id is required")
        String auth0UserId,

        Boolean emailVerified
) {
}