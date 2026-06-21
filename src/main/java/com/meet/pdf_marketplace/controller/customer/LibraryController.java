package com.meet.pdf_marketplace.controller.customer;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.library.DownloadAccessResponseDTO;
import com.meet.pdf_marketplace.dto.library.PurchasedPdfResponseDTO;
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
@RequestMapping("/api/v1/customer/library")
public class LibraryController {

    private final PurchasedPdfService purchasedPdfService;

    private final CurrentUserService currentUserService;

    /**
     * Lists products in the current customer's purchased library.
     */
    @GetMapping
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
     * Checks whether the current customer can access a product download.
     */
    @GetMapping("/products/{productId}/access")
    public ApiResponseDTO<DownloadAccessResponseDTO> hasAccess(
            @PathVariable UUID productId
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        DownloadAccessResponseDTO access = purchasedPdfService.hasAccess(currentUser, productId);

        return ApiResponseDTO.<DownloadAccessResponseDTO>builder()
                .success(true)
                .message("Product file access checked successfully")
                .data(access)
                .build();
    }

    /**
     * Generates a signed download URL when access is allowed.
     */
    @GetMapping("/products/{productId}/download")
    public ApiResponseDTO<DownloadAccessResponseDTO> download(
            @PathVariable UUID productId
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        DownloadAccessResponseDTO download = purchasedPdfService.getDownload(currentUser, productId);

        return ApiResponseDTO.<DownloadAccessResponseDTO>builder()
                .success(true)
                .message("Product file download access checked successfully")
                .data(download)
                .build();
    }
}

