package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.DownloadAccessResponseDTO;
import com.meet.pdf_marketplace.dto.GenerateDownloadUrlResponseDTO;
import com.meet.pdf_marketplace.dto.PurchasedPdfResponseDTO;
import com.meet.pdf_marketplace.entity.PdfProductEntity;
import com.meet.pdf_marketplace.entity.PurchasedPdfEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.PdfProductRepository;
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

    private final PdfProductRepository pdfProductRepository;

    private final R2StorageService r2StorageService;

    /**
     * Lists all PDFs purchased by a user.
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
     * Checks whether a user can access a product PDF.
     * Sellers can access their own uploads, and buyers need a purchase record.
     */
    @Transactional(readOnly = true)
    public DownloadAccessResponseDTO hasAccess(UserEntity currentUser, UUID productId) {

        PdfProductEntity product = findProduct(productId);

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

        PdfProductEntity product = findProduct(productId);

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
    private PdfProductEntity findProduct(UUID productId) {

        return pdfProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /**
     * Converts a purchased PDF entity into the library response DTO.
     */
    private PurchasedPdfResponseDTO toResponse(PurchasedPdfEntity purchasedPdf) {

        PdfProductEntity product = purchasedPdf.getProduct();

        return PurchasedPdfResponseDTO.builder()
                .id(purchasedPdf.getId())
                .userId(purchasedPdf.getUser().getId())
                .productId(product.getId())
                .orderId(purchasedPdf.getOrder().getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .thumbnailKey(product.getThumbnailKey())
                .category(product.getCategory())
                .accessGrantedAt(purchasedPdf.getAccessGrantedAt())
                .build();
    }

    /**
     * Checks seller ownership or purchased PDF access.
     */
    private boolean canAccess(UserEntity currentUser, PdfProductEntity product) {

        if (product.getSeller().getId().equals(currentUser.getId())) {
            return true;
        }

        return purchasedPdfRepository.existsByUserIdAndProductId(currentUser.getId(), product.getId());
    }
}
