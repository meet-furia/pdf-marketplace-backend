package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.auth.RegisterUserRequestDTO;
import com.meet.pdf_marketplace.dto.user.UserResponseDTO;
import com.meet.pdf_marketplace.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new local user or updates an existing Supabase-linked user.
     * Returns the saved user wrapped in the standard API response.
     */
    @PostMapping("/register")
    public ApiResponseDTO<UserResponseDTO> register(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegisterUserRequestDTO request
    ) {

        UserResponseDTO user = authService.register(jwt, request);

        return ApiResponseDTO.<UserResponseDTO>builder()
                .success(true)
                .message("User registered successfully")
                .data(user)
                .build();
    }
}

