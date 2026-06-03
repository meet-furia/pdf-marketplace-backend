package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.GenerateDownloadUrlResponseDTO;
import com.meet.pdf_marketplace.dto.GenerateUploadUrlRequestDTO;
import com.meet.pdf_marketplace.dto.GenerateUploadUrlResponseDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CurrentUserService;
import com.meet.pdf_marketplace.service.R2StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private final R2StorageService r2StorageService;

    private final CurrentUserService currentUserService;

    /**
     * Generates a presigned upload URL for the current user.
     * The frontend uploads directly to Cloudflare R2 with this URL.
     */
    @PostMapping("/upload-url")
    public ApiResponseDTO<GenerateUploadUrlResponseDTO> generateUploadUrl(
            @Valid @RequestBody GenerateUploadUrlRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        GenerateUploadUrlResponseDTO uploadUrl = r2StorageService.generateUploadUrl(currentUser, request);

        return ApiResponseDTO.<GenerateUploadUrlResponseDTO>builder()
                .success(true)
                .message("Upload URL generated successfully")
                .data(uploadUrl)
                .build();
    }

    /**
     * Generates a short-lived download URL after access is verified.
     * Files remain private in Cloudflare R2.
     */
    @GetMapping("/download-url")
    public ApiResponseDTO<GenerateDownloadUrlResponseDTO> generateDownloadUrl(
            @RequestParam String fileKey
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        GenerateDownloadUrlResponseDTO downloadUrl = r2StorageService.generateDownloadUrl(currentUser, fileKey);

        return ApiResponseDTO.<GenerateDownloadUrlResponseDTO>builder()
                .success(true)
                .message("Download URL generated successfully")
                .data(downloadUrl)
                .build();
    }
}
