package com.meet.pdf_marketplace.controller.seller;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.product.ProductResponseDTO;
import com.meet.pdf_marketplace.dto.product.UpdateProductRequestDTO;
import com.meet.pdf_marketplace.dto.product.UploadProductRequestDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CurrentUserService;
import com.meet.pdf_marketplace.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/seller/products")
public class SellerProductController {

    private final ProductService productService;

    private final CurrentUserService currentUserService;

    /**
     * Lists products owned by the current seller.
     */
    @GetMapping
    public ApiResponseDTO<List<ProductResponseDTO>> getMyProducts() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        List<ProductResponseDTO> products = productService.getMyProducts(currentUser);

        return ApiResponseDTO.<List<ProductResponseDTO>>builder()
                .success(true)
                .message("Seller products fetched successfully")
                .data(products)
                .build();
    }

    /**
     * Uploads a product file and creates a seller product for review.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseDTO<ProductResponseDTO> uploadAndCreate(
            @Valid @ModelAttribute UploadProductRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        ProductResponseDTO product = productService.createWithFiles(currentUser, request);

        return ApiResponseDTO.<ProductResponseDTO>builder()
                .success(true)
                .message("Product uploaded and created successfully")
                .data(product)
                .build();
    }

    /**
     * Updates product metadata owned by the current seller.
     */
    @PutMapping("/{productId}")
    public ApiResponseDTO<ProductResponseDTO> update(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        ProductResponseDTO product = productService.update(currentUser, productId, request);

        return ApiResponseDTO.<ProductResponseDTO>builder()
                .success(true)
                .message("Product updated successfully")
                .data(product)
                .build();
    }
}
