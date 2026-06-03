package com.meet.pdf_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateUploadUrlRequestDTO {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Content type is required")
    private String contentType;

    @NotBlank(message = "File type is required")
    private String fileType;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;
}
