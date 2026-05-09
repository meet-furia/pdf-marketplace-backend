package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.RegisterUserRequestDTO;
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
     * Registers or updates a user after successful Supabase authentication.
     */
    @PostMapping("/register")
    public ApiResponseDTO<Void> register(
            @Valid @RequestBody RegisterUserRequestDTO request
    ) {

        authService.register(request);

        return ApiResponseDTO.<Void>builder()
                .success(true)
                .message("User registered successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }
}