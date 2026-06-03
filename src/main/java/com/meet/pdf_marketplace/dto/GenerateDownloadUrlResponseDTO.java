package com.meet.pdf_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDownloadUrlResponseDTO {

    private String downloadUrl;

    private String fileKey;

    private LocalDateTime expiresAt;
}
