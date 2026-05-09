package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.Auth0RegisterRequestDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.UserStatus;
import com.meet.pdf_marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    /**
     * Creates or updates a user after successful Auth0 authentication.
     */
    @Transactional
    public void register(Auth0RegisterRequestDTO request) {

        UserEntity existingUser = userRepository
                .findByAuth0Id(request.auth0UserId())
                .orElse(null);

        // Update existing user details if user already exists
        if (existingUser != null) {

            existingUser.setEmail(request.email());
            existingUser.setName(request.name());
            existingUser.setEmailVerified(request.emailVerified());

            userRepository.save(existingUser);

            return;
        }

        // Create new user
        UserEntity user = UserEntity.builder()
                .auth0Id(request.auth0UserId())
                .email(request.email())
                .name(request.name())
                .emailVerified(
                        request.emailVerified() != null
                                ? request.emailVerified()
                                : false
                )
                .status(UserStatus.ACTIVE)
                .admin(false)
                .build();

        userRepository.save(user);
    }
}