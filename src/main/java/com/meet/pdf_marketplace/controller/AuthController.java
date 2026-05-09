package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.Auth0RegisterRequestDTO;
import com.meet.pdf_marketplace.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers or updates a user after successful Auth0 authentication.
     */
    @PostMapping("/register")
    public ApiResponseDTO<Void> register(
            @Valid @RequestBody Auth0RegisterRequestDTO request
    ) {

        authService.register(request);

        return ApiResponseDTO.<Void>builder()
                .success(true)
                .message("User registered successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }
}