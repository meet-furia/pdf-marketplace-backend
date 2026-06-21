package com.meet.pdf_marketplace.controller.customer;

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
@RequestMapping("/api/v1/customer/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers or syncs the current authenticated customer.
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

