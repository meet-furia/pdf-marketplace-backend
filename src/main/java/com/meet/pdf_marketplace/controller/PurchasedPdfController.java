package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.DownloadAccessResponseDTO;
import com.meet.pdf_marketplace.dto.PurchasedPdfResponseDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CurrentUserService;
import com.meet.pdf_marketplace.service.PurchasedPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/library")
public class PurchasedPdfController {

    private final PurchasedPdfService purchasedPdfService;

    private final CurrentUserService currentUserService;

    /**
     * Lists PDFs available in a user's purchased library.
     * Download URLs are not generated yet.
     */
    @GetMapping("/me")
    public ApiResponseDTO<List<PurchasedPdfResponseDTO>> getMyLibrary() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        List<PurchasedPdfResponseDTO> library = purchasedPdfService.getUserLibrary(currentUser);

        return ApiResponseDTO.<List<PurchasedPdfResponseDTO>>builder()
                .success(true)
                .message("Purchased library fetched successfully")
                .data(library)
                .build();
    }

    /**
     * Checks whether a user can access a product PDF.
     * Returns only true or false for now.
     */
    @GetMapping("/products/{productId}/access")
    public ApiResponseDTO<DownloadAccessResponseDTO> hasAccess(
            @PathVariable UUID productId
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        DownloadAccessResponseDTO access = purchasedPdfService.hasAccess(currentUser, productId);

        return ApiResponseDTO.<DownloadAccessResponseDTO>builder()
                .success(true)
                .message("PDF access checked successfully")
                .data(access)
                .build();
    }

    /**
     * Generates a signed download URL when access is allowed.
     * The file key is never exposed in the response.
     */
    @GetMapping("/products/{productId}/download")
    public ApiResponseDTO<DownloadAccessResponseDTO> download(
            @PathVariable UUID productId
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        DownloadAccessResponseDTO download = purchasedPdfService.getDownload(currentUser, productId);

        return ApiResponseDTO.<DownloadAccessResponseDTO>builder()
                .success(true)
                .message("PDF download access checked successfully")
                .data(download)
                .build();
    }
}
