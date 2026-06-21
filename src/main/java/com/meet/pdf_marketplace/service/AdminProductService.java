package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.product.ProductResponseDTO;
import com.meet.pdf_marketplace.dto.product.RejectProductRequestDTO;
import com.meet.pdf_marketplace.entity.ProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.PdfProductStatus;
import com.meet.pdf_marketplace.exception.ForbiddenOperationException;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;

    /**
     * Lists products waiting for admin approval.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getPendingProducts(
            UserEntity currentUser,
            Pageable pageable
    ) {
        validateAdmin(currentUser);

        return productRepository
                .findByStatus(PdfProductStatus.PENDING_APPROVAL, pageable)
                .map(this::toDto);
    }

    /**
     * Approves a product and publishes it to the marketplace.
     */
    @Transactional
    public ProductResponseDTO approveProduct(UserEntity currentUser, UUID productId) {
        validateAdmin(currentUser);

        ProductEntity product = findProduct(productId);
        product.setStatus(PdfProductStatus.PUBLISHED);

        return toDto(productRepository.save(product));
    }

    /**
     * Rejects a product from publishing.
     */
    @Transactional
    public ProductResponseDTO rejectProduct(
            UserEntity currentUser,
            UUID productId,
            RejectProductRequestDTO request
    ) {
        validateAdmin(currentUser);

        ProductEntity product = findProduct(productId);
        product.setStatus(PdfProductStatus.REJECTED);

        return toDto(productRepository.save(product));
    }

    /**
     * Ensures the current user is an admin.
     */
    private void validateAdmin(UserEntity currentUser) {
        if (!Boolean.TRUE.equals(currentUser.getAdmin())) {
            throw new ForbiddenOperationException("Admin access is required");
        }
    }

    /**
     * Loads a product or throws if missing.
     */
    private ProductEntity findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /**
     * Converts product entity to response DTO.
     */
    private ProductResponseDTO toDto(ProductEntity product) {
        return ProductResponseDTO.builder()
                .createdAt(product.getCreatedAt())
                .createdBy(product.getCreatedBy())
                .id(product.getId())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getName())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .fileType(product.getFileType())
                .fileContentType(product.getFileContentType())
                .fileOriginalName(product.getFileOriginalName())
                .fileSizeBytes(product.getFileSizeBytes())
                .thumbnailKey(product.getThumbnailKey())
                .category(product.getCategory())
                .status(product.getStatus())
                .build();
    }
}
