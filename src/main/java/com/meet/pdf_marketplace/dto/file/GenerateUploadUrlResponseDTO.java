package com.meet.pdf_marketplace.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateUploadUrlResponseDTO {

    private String uploadUrl;

    private String uploadMethod;

    private String fileKey;

    private LocalDateTime expiresAt;
}

