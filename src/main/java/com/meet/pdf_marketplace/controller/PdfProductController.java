package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.CreatePdfProductRequestDTO;
import com.meet.pdf_marketplace.dto.PdfProductResponseDTO;
import com.meet.pdf_marketplace.dto.UpdatePdfProductRequestDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CurrentUserService;
import com.meet.pdf_marketplace.service.PdfProductService;
import com.meet.pdf_marketplace.util.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/products")
public class PdfProductController {

    private final PdfProductService pdfProductService;

    private final CurrentUserService currentUserService;

    /**
     * Lists published PDF products with pagination for the homepage.
     * Only products with PUBLISHED status are returned.
     */
    @GetMapping
    public ApiResponseDTO<Page<PdfProductResponseDTO>> getPublishedPage(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        Pageable pageable = Utils.validateAndCreatePageable(page, size, sortField, sortDirection);

        Page<PdfProductResponseDTO> products = pdfProductService.getPublished(pageable);

        return ApiResponseDTO.<Page<PdfProductResponseDTO>>builder()
                .success(true)
                .message("Published products fetched successfully")
                .data(products)
                .build();
    }

    /**
     * Creates a PDF product for a seller.
     * Returns the created product in the standard API response.
     */
    @PostMapping
    public ApiResponseDTO<PdfProductResponseDTO> create(
            @Valid @RequestBody CreatePdfProductRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        PdfProductResponseDTO product = pdfProductService.create(currentUser, request);

        return ApiResponseDTO.<PdfProductResponseDTO>builder()
                .success(true)
                .message("Product created successfully")
                .data(product)
                .build();
    }

    /**
     * Updates an existing PDF product.
     * Returns the updated product in the standard API response.
     */
    @PutMapping("/{productId}")
    public ApiResponseDTO<PdfProductResponseDTO> update(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdatePdfProductRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        PdfProductResponseDTO product = pdfProductService.update(currentUser, productId, request);

        return ApiResponseDTO.<PdfProductResponseDTO>builder()
                .success(true)
                .message("Product updated successfully")
                .data(product)
                .build();
    }

    /**
     * Gets one PDF product by id.
     * Returns product details in the standard API response.
     */
    @GetMapping("/{productId}")
    public ApiResponseDTO<PdfProductResponseDTO> getById(
            @PathVariable UUID productId
    ) {

        PdfProductResponseDTO product = pdfProductService.getById(productId);

        return ApiResponseDTO.<PdfProductResponseDTO>builder()
                .success(true)
                .message("Product fetched successfully")
                .data(product)
                .build();
    }

    /**
     * Lists published PDF products.
     * Only products with PUBLISHED status are returned.
     */
    @GetMapping("/published")
    public ApiResponseDTO<List<PdfProductResponseDTO>> getPublished() {

        List<PdfProductResponseDTO> products = pdfProductService.getPublished();

        return ApiResponseDTO.<List<PdfProductResponseDTO>>builder()
                .success(true)
                .message("Published products fetched successfully")
                .data(products)
                .build();
    }

    /**
     * Lists PDF products for the current seller.
     * The current user comes from the Supabase JWT.
     */
    @GetMapping("/me")
    public ApiResponseDTO<List<PdfProductResponseDTO>> getMyProducts() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        List<PdfProductResponseDTO> products = pdfProductService.getMyProducts(currentUser);

        return ApiResponseDTO.<List<PdfProductResponseDTO>>builder()
                .success(true)
                .message("Seller products fetched successfully")
                .data(products)
                .build();
    }

}
