package com.meet.pdf_marketplace.controller.admin;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.product.ProductResponseDTO;
import com.meet.pdf_marketplace.dto.product.RejectProductRequestDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.AdminProductService;
import com.meet.pdf_marketplace.service.CurrentUserService;
import com.meet.pdf_marketplace.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

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
     */
    @GetMapping("/pending")
    public ApiResponseDTO<Page<ProductResponseDTO>> getPendingProducts(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();

        Pageable pageable = Utils.validateAndCreatePageable(page, size, sortField, sortDirection);

        Page<ProductResponseDTO> products =
                adminProductService.getPendingProducts(currentUser, pageable);

        return ApiResponseDTO.<Page<ProductResponseDTO>>builder()
                .success(true)
                .message("Pending products fetched successfully")
                .data(products)
                .build();
    }

    /**
     * Approves and publishes a product.
     */
    @PatchMapping("/{productId}/approve")
    public ApiResponseDTO<ProductResponseDTO> approveProduct(
            @PathVariable UUID productId
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();

        ProductResponseDTO product =
                adminProductService.approveProduct(currentUser, productId);

        return ApiResponseDTO.<ProductResponseDTO>builder()
                .success(true)
                .message("Product approved successfully")
                .data(product)
                .build();
    }

    /**
     * Rejects a product from publishing.
     */
    @PatchMapping("/{productId}/reject")
    public ApiResponseDTO<ProductResponseDTO> rejectProduct(
            @PathVariable UUID productId,
            @RequestBody RejectProductRequestDTO request
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();

        ProductResponseDTO product =
                adminProductService.rejectProduct(currentUser, productId, request);

        return ApiResponseDTO.<ProductResponseDTO>builder()
                .success(true)
                .message("Product rejected successfully")
                .data(product)
                .build();
    }
}