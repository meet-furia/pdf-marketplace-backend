package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.auth.RegisterUserRequestDTO;
import com.meet.pdf_marketplace.dto.user.UserResponseDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.UserStatus;
import com.meet.pdf_marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final SupabaseJwtService supabaseJwtService;

    /**
     * Creates or updates a local user from Supabase Auth details.
     * Keeps registration idempotent and returns the saved user.
     */
    @Transactional
    public UserResponseDTO register(Jwt jwt, RegisterUserRequestDTO request) {

        if (jwt == null) {
            throw new AuthenticationCredentialsNotFoundException("Supabase JWT is required for registration");
        }

        String supabaseUserId = supabaseJwtService.getSupabaseUserId(jwt);
        String email = supabaseJwtService.getEmail(jwt);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Supabase JWT email is missing");
        }

        UserEntity user = userRepository.findBySupabaseUserId(supabaseUserId)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .orElseGet(() -> UserEntity.builder()
                        .status(UserStatus.ACTIVE)
                        .build());

        user.setSupabaseUserId(supabaseUserId);
        user.setEmail(email);
        user.setName(request.getName());
        user.setEmailVerified(Boolean.TRUE.equals(request.getEmailVerified()));

        return toResponse(userRepository.save(user));
    }

    /**
     * Converts the persisted user entity into the auth response DTO.
     */
    private UserResponseDTO toResponse(UserEntity user) {

        return UserResponseDTO.builder()
                .id(user.getId())
                .supabaseUserId(user.getSupabaseUserId())
                .email(user.getEmail())
                .name(user.getName())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .admin(user.getAdmin())
                .build();
    }
}

