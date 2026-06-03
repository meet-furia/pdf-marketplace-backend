package com.meet.pdf_marketplace.dto;

import com.meet.pdf_marketplace.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private UUID id;

    private String supabaseUserId;

    private String email;

    private String name;

    private UserStatus status;

    private Boolean emailVerified;
}
