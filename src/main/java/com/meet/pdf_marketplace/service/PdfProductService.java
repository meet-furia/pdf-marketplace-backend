package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.CreatePdfProductRequestDTO;
import com.meet.pdf_marketplace.dto.PdfProductResponseDTO;
import com.meet.pdf_marketplace.dto.UpdatePdfProductRequestDTO;
import com.meet.pdf_marketplace.entity.PdfProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.PdfProductStatus;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.PdfProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfProductService {

    private final PdfProductRepository pdfProductRepository;

    /**
     * Creates a PDF product for the current seller.
     * Returns the saved product details.
     */
    @Transactional
    public PdfProductResponseDTO create(UserEntity currentUser, CreatePdfProductRequestDTO request) {

        validateProductFileKeys(currentUser, request.getFileKey(), request.getThumbnailKey());

        PdfProductEntity product = PdfProductEntity.builder()
                .seller(currentUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .fileKey(request.getFileKey())
                .thumbnailKey(request.getThumbnailKey())
                .category(request.getCategory())
                .status(PdfProductStatus.DRAFT)
                .build();

        return toSellerResponse(pdfProductRepository.save(product));
    }

    /**
     * Updates a PDF product owned by the current seller.
     * Returns the latest product details.
     */
    @Transactional
    public PdfProductResponseDTO update(
            UserEntity currentUser,
            UUID productId,
            UpdatePdfProductRequestDTO request
    ) {

        PdfProductEntity product = findProduct(productId);
        validateSeller(currentUser, product);
        validateProductFileKeys(currentUser, request.getFileKey(), request.getThumbnailKey());

        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setFileKey(request.getFileKey());
        product.setThumbnailKey(request.getThumbnailKey());
        product.setCategory(request.getCategory());
        product.setStatus(resolveSellerStatus(request.getStatus()));

        return toSellerResponse(pdfProductRepository.save(product));
    }

    /**
     * Finds a PDF product by id.
     * Returns product details without exposing the entity.
     */
    @Transactional(readOnly = true)
    public PdfProductResponseDTO getById(UUID productId) {

        PdfProductEntity product = findProduct(productId);

        if (product.getStatus() != PdfProductStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Product not found");
        }

        return toPublicResponse(product);
    }

    /**
     * Lists all published PDF products.
     * Draft, unpublished, and deleted products are excluded.
     */
    @Transactional(readOnly = true)
    public List<PdfProductResponseDTO> getPublished() {

        return pdfProductRepository.findByStatus(PdfProductStatus.PUBLISHED)
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    /**
     * Lists published PDF products with pagination for the homepage.
     * Draft, unpublished, rejected, and deleted products are excluded.
     */
    @Transactional(readOnly = true)
    public Page<PdfProductResponseDTO> getPublished(Pageable pageable) {

        return pdfProductRepository.findByStatus(PdfProductStatus.PUBLISHED, pageable)
                .map(this::toPublicResponse);
    }

    /**
     * Lists all PDF products for the current seller.
     * Returns products in any status for that seller.
     */
    @Transactional(readOnly = true)
    public List<PdfProductResponseDTO> getMyProducts(UserEntity currentUser) {

        return pdfProductRepository.findBySellerId(currentUser.getId())
                .stream()
                .map(this::toSellerResponse)
                .toList();
    }

    /**
     * Ensures only the seller can mutate their product.
     */
    private void validateSeller(UserEntity currentUser, PdfProductEntity product) {

        if (!product.getSeller().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Current user does not own this product");
        }
    }

    /**
     * Loads a product or fails when the product does not exist.
     */
    private PdfProductEntity findProduct(UUID productId) {

        return pdfProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /**
     * Ensures product file keys belong to the current seller's upload paths.
     */
    private void validateProductFileKeys(UserEntity currentUser, String fileKey, String thumbnailKey) {

        String pdfPrefix = "products/pdfs/" + currentUser.getId() + "/";

        if (fileKey == null || !fileKey.startsWith(pdfPrefix)) {
            throw new IllegalArgumentException("PDF file key does not belong to the current user");
        }

        if (thumbnailKey != null && !thumbnailKey.isBlank()) {
            String thumbnailPrefix = "products/thumbnails/" + currentUser.getId() + "/";

            if (!thumbnailKey.startsWith(thumbnailPrefix)) {
                throw new IllegalArgumentException("Thumbnail file key does not belong to the current user");
            }
        }
    }

    /**
     * Prevents sellers from publishing without admin approval.
     */
    private PdfProductStatus resolveSellerStatus(PdfProductStatus requestedStatus) {

        if (requestedStatus == PdfProductStatus.PUBLISHED) {
            return PdfProductStatus.PENDING_APPROVAL;
        }

        return requestedStatus;
    }

    /**
     * Converts the product entity into the public response DTO.
     */
    private PdfProductResponseDTO toPublicResponse(PdfProductEntity product) {

        return PdfProductResponseDTO.builder()
                .id(product.getId())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getName())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .thumbnailKey(product.getThumbnailKey())
                .category(product.getCategory())
                .status(product.getStatus())
                .build();
    }

    /**
     * Converts the product entity into the seller-owned response DTO.
     */
    private PdfProductResponseDTO toSellerResponse(PdfProductEntity product) {

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
