package com.meet.pdf_marketplace.controller.customer;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.product.ProductResponseDTO;
import com.meet.pdf_marketplace.service.ProductService;
import com.meet.pdf_marketplace.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/customer/products")
public class CustomerProductController {

    private final ProductService productService;

    /**
     * Lists published products for marketplace browsing.
     */
    @GetMapping
    public ApiResponseDTO<Page<ProductResponseDTO>> getPublishedPage(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        Pageable pageable = Utils.validateAndCreatePageable(page, size, sortField, sortDirection);
        Page<ProductResponseDTO> products = productService.getPublished(pageable);

        return ApiResponseDTO.<Page<ProductResponseDTO>>builder()
                .success(true)
                .message("Published products fetched successfully")
                .data(products)
                .build();
    }

    /**
     * Gets one published product by id.
     */
    @GetMapping("/{productId}")
    public ApiResponseDTO<ProductResponseDTO> getById(
            @PathVariable UUID productId
    ) {

        ProductResponseDTO product = productService.getById(productId);

        return ApiResponseDTO.<ProductResponseDTO>builder()
                .success(true)
                .message("Product fetched successfully")
                .data(product)
                .build();
    }
}
