package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.product.ProductResponseDTO;
import com.meet.pdf_marketplace.dto.product.UpdateProductRequestDTO;
import com.meet.pdf_marketplace.dto.product.UploadProductRequestDTO;
import com.meet.pdf_marketplace.entity.ProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.PdfProductStatus;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final R2StorageService r2StorageService;

    /**
     * Uploads product files to R2 and creates a product pending admin approval.
     */
    @Transactional
    public ProductResponseDTO createWithFiles(UserEntity currentUser, UploadProductRequestDTO request) {

        R2StorageService.StoredFile productFile = null;
        R2StorageService.StoredFile thumbnailFile = null;

        try {
            log.info("Creating product upload. sellerId={}, title={}", currentUser.getId(), request.getTitle());

            productFile = r2StorageService.uploadProductFile(currentUser, request.getProductFile());

            if (request.getThumbnailFile() != null && !request.getThumbnailFile().isEmpty()) {
                thumbnailFile = r2StorageService.uploadThumbnailFile(currentUser, request.getThumbnailFile());
            }

            ProductEntity product = ProductEntity.builder()
                    .seller(currentUser)
                    .title(request.getTitle().trim())
                    .description(request.getDescription().trim())
                    .price(request.getPrice())
                    .fileKey(productFile.fileKey())
                    .fileType(productFile.fileType())
                    .fileContentType(productFile.contentType())
                    .fileOriginalName(productFile.originalName())
                    .fileSizeBytes(productFile.sizeBytes())
                    .thumbnailKey(thumbnailFile == null ? null : thumbnailFile.fileKey())
                    .category(normalizeOptionalText(request.getCategory()))
                    // New seller uploads must go through admin approval before becoming public.
                    .status(PdfProductStatus.PENDING_APPROVAL)
                    .build();

            ProductEntity savedProduct = productRepository.save(product);

            log.info("Product created successfully. productId={}, sellerId={}", savedProduct.getId(), currentUser.getId());

            return toDto(savedProduct);
        } catch (RuntimeException exception) {
            log.error("Product creation failed. sellerId={}", currentUser.getId(), exception);

            // If R2 upload succeeded but DB save failed, remove orphaned uploaded files.
            cleanupUploadedFile(productFile);
            cleanupUploadedFile(thumbnailFile);

            throw exception;
        }
    }

    /**
     * Updates product metadata owned by the current seller.
     */
    @Transactional
    public ProductResponseDTO update(
            UserEntity currentUser,
            UUID productId,
            UpdateProductRequestDTO request
    ) {

        ProductEntity product = findProduct(productId);

        validateSeller(currentUser, product);

        product.setTitle(request.getTitle().trim());
        product.setDescription(request.getDescription().trim());
        product.setPrice(request.getPrice());
        product.setCategory(normalizeOptionalText(request.getCategory()));
        product.setStatus(resolveSellerStatus(request.getStatus()));

        ProductEntity savedProduct = productRepository.save(product);

        log.info("Product updated successfully. productId={}, sellerId={}", savedProduct.getId(), currentUser.getId());

        return toDto(savedProduct);
    }

    /**
     * Gets one published product by id.
     */
    @Transactional(readOnly = true)
    public ProductResponseDTO getById(UUID productId) {

        ProductEntity product = findProduct(productId);

        if (product.getStatus() != PdfProductStatus.PUBLISHED) {
            // Non-published products should not be discoverable from public product detail APIs.
            throw new ResourceNotFoundException("Product not found");
        }

        return toDto(product);
    }

    /**
     * Lists all published products.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getPublished() {

        return productRepository.findByStatus(PdfProductStatus.PUBLISHED)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Lists published products with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getPublished(Pageable pageable) {

        return productRepository.findByStatus(PdfProductStatus.PUBLISHED, pageable)
                .map(this::toDto);
    }

    /**
     * Lists all products created by the current seller.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getMyProducts(UserEntity currentUser) {

        return productRepository.findBySellerId(currentUser.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Ensures only the product owner can update the product.
     */
    private void validateSeller(UserEntity currentUser, ProductEntity product) {

        if (!product.getSeller().getId().equals(currentUser.getId())) {
            // Sellers must not be able to update products owned by other sellers.
            throw new IllegalArgumentException("Current user does not own this product");
        }
    }

    private void cleanupUploadedFile(R2StorageService.StoredFile file) {

        try {
            r2StorageService.deleteFile(file == null ? null : file.fileKey());
        } catch (RuntimeException exception) {
            // Cleanup failure should not hide the original product creation failure.
            log.warn("Failed to cleanup uploaded file after product creation failure", exception);
        }
    }

    private String normalizeOptionalText(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Loads a product or fails if it does not exist.
     */
    private ProductEntity findProduct(UUID productId) {

        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /**
     * Prevents sellers from publishing without admin approval.
     */
    private PdfProductStatus resolveSellerStatus(PdfProductStatus requestedStatus) {

        if (requestedStatus == null) {
            return PdfProductStatus.PENDING_APPROVAL;
        }

        if (requestedStatus == PdfProductStatus.PUBLISHED) {
            // Sellers cannot directly publish products. Admin approval is required.
            return PdfProductStatus.PENDING_APPROVAL;
        }

        return requestedStatus;
    }

    /**
     * Converts product entity to response DTO without exposing backend-only fileKey.
     */
    private ProductResponseDTO toDto(ProductEntity product) {

        // fileKey stays backend-only. Downloads must always go through signed R2 URLs.
        return ProductResponseDTO.builder()
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
                .thumbnailUrl(r2StorageService.generateAssetUrl(product.getThumbnailKey()))
                .category(product.getCategory())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .createdBy(product.getCreatedBy())
                .build();
    }
}
