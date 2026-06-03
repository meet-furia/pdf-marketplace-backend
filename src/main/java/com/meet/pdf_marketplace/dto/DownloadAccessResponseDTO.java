package com.meet.pdf_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadAccessResponseDTO {

    private UUID productId;

    private Boolean access;

    private String downloadUrl;

    private LocalDateTime expiresAt;
}
