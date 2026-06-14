package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.product.PdfProductResponseDTO;
import com.meet.pdf_marketplace.dto.product.RejectProductRequestDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.AdminProductService;
import com.meet.pdf_marketplace.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    private final CurrentUserService currentUserService;

    /**
     * Lists products pending admin approval.
     * The current user's email must be configured as admin.
     */
    @GetMapping("/pending")
    public ApiResponseDTO<List<PdfProductResponseDTO>> getPendingProducts() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        List<PdfProductResponseDTO> products = adminProductService.getPendingProducts(currentUser);

        return ApiResponseDTO.<List<PdfProductResponseDTO>>builder()
                .success(true)
                .message("Pending products fetched successfully")
                .data(products)
                .build();
    }

    /**
     * Approves and publishes a product.
     * The current user's email must be configured as admin.
     */
    @PutMapping("/{productId}/approve")
    public ApiResponseDTO<PdfProductResponseDTO> approveProduct(
            @PathVariable UUID productId
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        PdfProductResponseDTO product = adminProductService.approveProduct(currentUser, productId);

        return ApiResponseDTO.<PdfProductResponseDTO>builder()
                .success(true)
                .message("Product approved successfully")
                .data(product)
                .build();
    }

    /**
     * Rejects a product from publishing.
     * The current user's email must be configured as admin.
     */
    @PutMapping("/{productId}/reject")
    public ApiResponseDTO<PdfProductResponseDTO> rejectProduct(
            @PathVariable UUID productId,
            @RequestBody RejectProductRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        PdfProductResponseDTO product = adminProductService.rejectProduct(currentUser, productId, request);

        return ApiResponseDTO.<PdfProductResponseDTO>builder()
                .success(true)
                .message("Product rejected successfully")
                .data(product)
                .build();
    }
}

