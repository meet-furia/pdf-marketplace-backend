package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.file.GenerateDownloadUrlResponseDTO;
import com.meet.pdf_marketplace.dto.library.DownloadAccessResponseDTO;
import com.meet.pdf_marketplace.dto.library.PurchasedPdfResponseDTO;
import com.meet.pdf_marketplace.entity.ProductEntity;
import com.meet.pdf_marketplace.entity.PurchasedPdfEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.ProductRepository;
import com.meet.pdf_marketplace.repository.PurchasedPdfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchasedPdfService {

    private final PurchasedPdfRepository purchasedPdfRepository;

    private final ProductRepository productRepository;

    private final R2StorageService r2StorageService;

    /**
     * Lists all digital products purchased by a user.
     * Returns library items without exposing entities.
     */
    @Transactional(readOnly = true)
    public List<PurchasedPdfResponseDTO> getUserLibrary(UserEntity currentUser) {

        return purchasedPdfRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Checks whether a user can access a product file.
     * Sellers can access their own uploads, and buyers need a purchase record.
     */
    @Transactional(readOnly = true)
    public DownloadAccessResponseDTO hasAccess(UserEntity currentUser, UUID productId) {

        ProductEntity product = findProduct(productId);

        return DownloadAccessResponseDTO.builder()
                .productId(productId)
                .access(canAccess(currentUser, product))
                .build();
    }

    /**
     * Generates a download URL when the current user has access.
     */
    @Transactional(readOnly = true)
    public DownloadAccessResponseDTO getDownload(UserEntity currentUser, UUID productId) {

        ProductEntity product = findProduct(productId);

        if (!canAccess(currentUser, product)) {
            return DownloadAccessResponseDTO.builder()
                    .productId(productId)
                    .access(false)
                    .build();
        }

        GenerateDownloadUrlResponseDTO downloadUrl = r2StorageService.generateAuthorizedDownloadUrl(product.getFileKey());

        return DownloadAccessResponseDTO.builder()
                .productId(productId)
                .access(true)
                .downloadUrl(downloadUrl.getDownloadUrl())
                .expiresAt(downloadUrl.getExpiresAt())
                .build();
    }

    /**
     * Loads a product or fails when the product does not exist.
     */
    private ProductEntity findProduct(UUID productId) {

        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /**
     * Converts a purchased product entity into the library response DTO.
     */
    private PurchasedPdfResponseDTO toResponse(PurchasedPdfEntity purchasedPdf) {

        ProductEntity product = purchasedPdf.getProduct();

        return PurchasedPdfResponseDTO.builder()
                .id(purchasedPdf.getId())
                .userId(purchasedPdf.getUser().getId())
                .productId(product.getId())
                .orderId(purchasedPdf.getOrder().getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .fileType(product.getFileType())
                .fileContentType(product.getFileContentType())
                .fileOriginalName(product.getFileOriginalName())
                .fileSizeBytes(product.getFileSizeBytes())
                .thumbnailKey(product.getThumbnailKey())
                .category(product.getCategory())
                .accessGrantedAt(purchasedPdf.getAccessGrantedAt())
                .build();
    }

    /**
     * Checks seller ownership or purchased product access.
     */
    private boolean canAccess(UserEntity currentUser, ProductEntity product) {

        if (product.getSeller().getId().equals(currentUser.getId())) {
            return true;
        }

        return purchasedPdfRepository.existsByUserIdAndProductId(currentUser.getId(), product.getId());
    }
}

