package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.PdfProductResponseDTO;
import com.meet.pdf_marketplace.dto.RejectProductRequestDTO;
import com.meet.pdf_marketplace.entity.PdfProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.PdfProductStatus;
import com.meet.pdf_marketplace.exception.ForbiddenOperationException;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.PdfProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final PdfProductRepository pdfProductRepository;

    @Value("${admin.emails:}")
    private String adminEmails;

    /**
     * Lists products waiting for admin approval.
     * Only configured admin emails can access this list.
     */
    @Transactional(readOnly = true)
    public List<PdfProductResponseDTO> getPendingProducts(UserEntity currentUser) {

        validateAdmin(currentUser);

        return pdfProductRepository.findByStatus(PdfProductStatus.PENDING_APPROVAL)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Approves a product and publishes it to the marketplace.
     * Only configured admin emails can approve products.
     */
    @Transactional
    public PdfProductResponseDTO approveProduct(UserEntity currentUser, UUID productId) {

        validateAdmin(currentUser);

        PdfProductEntity product = findProduct(productId);
        product.setStatus(PdfProductStatus.PUBLISHED);

        return toResponse(pdfProductRepository.save(product));
    }

    /**
     * Rejects a product from publishing.
     * Only configured admin emails can reject products.
     */
    @Transactional
    public PdfProductResponseDTO rejectProduct(
            UserEntity currentUser,
            UUID productId,
            RejectProductRequestDTO request
    ) {

        validateAdmin(currentUser);

        PdfProductEntity product = findProduct(productId);
        product.setStatus(PdfProductStatus.REJECTED);

        return toResponse(pdfProductRepository.save(product));
    }

    /**
     * Ensures the current user's email is configured as an admin.
     */
    private void validateAdmin(UserEntity currentUser) {

        boolean admin = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .anyMatch(email -> email.equalsIgnoreCase(currentUser.getEmail()));

        if (!admin) {
            throw new ForbiddenOperationException("Admin access is required");
        }
    }

    /**
     * Loads a product or fails when missing.
     */
    private PdfProductEntity findProduct(UUID productId) {

        return pdfProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /**
     * Converts product entity into admin response DTO.
     */
    private PdfProductResponseDTO toResponse(PdfProductEntity product) {

        return PdfProductResponseDTO.builder()
                .id(product.getId())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getName())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .fileKey(product.getFileKey())
                .thumbnailKey(product.getThumbnailKey())
                .category(product.getCategory())
                .status(product.getStatus())
                .build();
    }
}
