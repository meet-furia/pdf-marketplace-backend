package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.user.UserResponseDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/users")
public class UserController {

    private final CurrentUserService currentUserService;

    /**
     * Returns the local user mapped to the current Supabase JWT.
     */
    @GetMapping("/me")
    public ApiResponseDTO<UserResponseDTO> getMe() {

        UserEntity currentUser = currentUserService.getCurrentUser();

        return ApiResponseDTO.<UserResponseDTO>builder()
                .success(true)
                .message("Current user fetched successfully")
                .data(toResponse(currentUser))
                .build();
    }

    /**
     * Converts the current user entity into a response DTO.
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

