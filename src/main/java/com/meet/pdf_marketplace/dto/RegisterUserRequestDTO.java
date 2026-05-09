package com.meet.pdf_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequestDTO(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        String email,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Supabase user id is required")
        String supabaseUserId,

        Boolean emailVerified
) {
}