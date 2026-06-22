package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.product.ProductResponseDTO;
import com.meet.pdf_marketplace.dto.product.UpdateProductRequestDTO;
import com.meet.pdf_marketplace.entity.ProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.PdfProductStatus;
import com.meet.pdf_marketplace.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private R2StorageService r2StorageService;

    @InjectMocks
    private ProductService productService;

    /**
     * Clears manually initialized transaction synchronization after each test.
     */
    @AfterEach
    void clearTransactionSynchronization() {

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * Verifies that metadata-only updates preserve existing objects and status.
     */
    @Test
    void updateWithoutReplacementFilesPreservesExistingObjects() {

        UserEntity seller = buildSeller();
        ProductEntity product = buildProduct(seller);
        UpdateProductRequestDTO request = buildUpdateRequest(null, null);

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(r2StorageService.generateAssetUrl(product.getThumbnailKey())).thenReturn("thumbnail-url");

        ProductResponseDTO response = productService.update(seller, product.getId(), request);

        assertEquals("products/files/old-file.pdf", product.getFileKey());
        assertEquals("products/thumbnails/old-thumbnail.png", product.getThumbnailKey());
        assertEquals("Updated title", response.getTitle());
        assertEquals(PdfProductStatus.PUBLISHED, product.getStatus());
        verify(r2StorageService, never()).uploadProductFile(any(), any());
        verify(r2StorageService, never()).uploadThumbnailFile(any(), any());
        verify(r2StorageService, never()).deleteFile(any());
    }

    /**
     * Verifies that replacing only the thumbnail does not require admin approval.
     */
    @Test
    void thumbnailReplacementPreservesPublishedStatus() {

        TransactionSynchronizationManager.initSynchronization();

        UserEntity seller = buildSeller();
        ProductEntity product = buildProduct(seller);
        MultipartFile thumbnailUpload = mock(MultipartFile.class);
        UpdateProductRequestDTO request = buildUpdateRequest(null, thumbnailUpload);
        R2StorageService.StoredFile replacementThumbnail = new R2StorageService.StoredFile(
                "products/thumbnails/new-thumbnail.png",
                "THUMBNAIL",
                "image/png",
                "thumbnail.png",
                1024L
        );

        when(thumbnailUpload.isEmpty()).thenReturn(false);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(r2StorageService.uploadThumbnailFile(seller, thumbnailUpload)).thenReturn(replacementThumbnail);
        when(productRepository.save(product)).thenReturn(product);
        when(r2StorageService.generateAssetUrl(replacementThumbnail.fileKey())).thenReturn("thumbnail-url");

        productService.update(seller, product.getId(), request);

        assertEquals(replacementThumbnail.fileKey(), product.getThumbnailKey());
        assertEquals(PdfProductStatus.PUBLISHED, product.getStatus());
        verify(r2StorageService, never()).uploadProductFile(any(), any());
    }

    /**
     * Verifies that replacement metadata is saved before old objects are deleted.
     */
    @Test
    void successfulReplacementDeletesOldFilesOnlyAfterCommit() {

        TransactionSynchronizationManager.initSynchronization();

        UserEntity seller = buildSeller();
        ProductEntity product = buildProduct(seller);
        MultipartFile productUpload = mock(MultipartFile.class);
        MultipartFile thumbnailUpload = mock(MultipartFile.class);
        UpdateProductRequestDTO request = buildUpdateRequest(productUpload, thumbnailUpload);
        R2StorageService.StoredFile replacementProduct = new R2StorageService.StoredFile(
                "products/files/new-guide.zip",
                "ZIP",
                "application/zip",
                "guide.zip",
                2048L
        );
        R2StorageService.StoredFile replacementThumbnail = new R2StorageService.StoredFile(
                "products/thumbnails/new-thumbnail.png",
                "THUMBNAIL",
                "image/png",
                "thumbnail.png",
                1024L
        );

        when(productUpload.isEmpty()).thenReturn(false);
        when(thumbnailUpload.isEmpty()).thenReturn(false);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(r2StorageService.uploadProductFile(seller, productUpload)).thenReturn(replacementProduct);
        when(r2StorageService.uploadThumbnailFile(seller, thumbnailUpload)).thenReturn(replacementThumbnail);
        when(productRepository.save(product)).thenReturn(product);
        when(r2StorageService.generateAssetUrl(replacementThumbnail.fileKey())).thenReturn("thumbnail-url");

        ProductResponseDTO response = productService.update(seller, product.getId(), request);

        assertEquals(replacementProduct.fileKey(), product.getFileKey());
        assertEquals(replacementProduct.originalName(), response.getFileOriginalName());
        assertEquals(replacementThumbnail.fileKey(), product.getThumbnailKey());
        assertEquals(PdfProductStatus.PENDING_APPROVAL, product.getStatus());
        verify(r2StorageService, never()).deleteFile(any());

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());

        synchronizations.get(0).afterCommit();

        verify(r2StorageService).deleteFile("products/files/old-file.pdf");
        verify(r2StorageService).deleteFile("products/thumbnails/old-thumbnail.png");
    }

    /**
     * Verifies that newly uploaded replacements are removed when persistence fails.
     */
    @Test
    void failedReplacementDeletesNewFilesAndKeepsOldFiles() {

        UserEntity seller = buildSeller();
        ProductEntity product = buildProduct(seller);
        MultipartFile productUpload = mock(MultipartFile.class);
        MultipartFile thumbnailUpload = mock(MultipartFile.class);
        UpdateProductRequestDTO request = buildUpdateRequest(productUpload, thumbnailUpload);
        R2StorageService.StoredFile replacementProduct = new R2StorageService.StoredFile(
                "products/files/new-guide.zip",
                "ZIP",
                "application/zip",
                "guide.zip",
                2048L
        );
        R2StorageService.StoredFile replacementThumbnail = new R2StorageService.StoredFile(
                "products/thumbnails/new-thumbnail.png",
                "THUMBNAIL",
                "image/png",
                "thumbnail.png",
                1024L
        );

        when(productUpload.isEmpty()).thenReturn(false);
        when(thumbnailUpload.isEmpty()).thenReturn(false);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(r2StorageService.uploadProductFile(seller, productUpload)).thenReturn(replacementProduct);
        when(r2StorageService.uploadThumbnailFile(seller, thumbnailUpload)).thenReturn(replacementThumbnail);
        when(productRepository.save(product)).thenThrow(new IllegalStateException("Database unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> productService.update(seller, product.getId(), request)
        );

        verify(r2StorageService).deleteFile(replacementProduct.fileKey());
        verify(r2StorageService).deleteFile(replacementThumbnail.fileKey());
        verify(r2StorageService, never()).deleteFile("products/files/old-file.pdf");
        verify(r2StorageService, never()).deleteFile("products/thumbnails/old-thumbnail.png");
    }

    /**
     * Verifies that a seller cannot update another seller's product or upload files for it.
     */
    @Test
    void updateRejectsProductsOwnedByAnotherSeller() {

        UserEntity owner = buildSeller();
        UserEntity otherSeller = buildSeller();
        ProductEntity product = buildProduct(owner);

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.update(
                        otherSeller,
                        product.getId(),
                        buildUpdateRequest(mock(MultipartFile.class), null)
                )
        );

        verify(productRepository, never()).save(any());
        verify(r2StorageService, never()).uploadProductFile(any(), any());
    }

    /**
     * Creates a seller with the identifiers required by ownership and response mapping.
     */
    private UserEntity buildSeller() {

        UserEntity seller = UserEntity.builder()
                .name("Test Seller")
                .build();
        seller.setId(UUID.randomUUID());

        return seller;
    }

    /**
     * Creates a product containing existing R2 keys for replacement tests.
     */
    private ProductEntity buildProduct(UserEntity seller) {

        ProductEntity product = ProductEntity.builder()
                .seller(seller)
                .title("Original title")
                .description("Original description")
                .price(new BigDecimal("100.00"))
                .fileKey("products/files/old-file.pdf")
                .fileType("PDF")
                .fileContentType("application/pdf")
                .fileOriginalName("old-file.pdf")
                .fileSizeBytes(1024L)
                .thumbnailKey("products/thumbnails/old-thumbnail.png")
                .category("Guides")
                .status(PdfProductStatus.PUBLISHED)
                .build();
        product.setId(UUID.randomUUID());

        return product;
    }

    /**
     * Creates a valid update request with optional replacement files.
     */
    private UpdateProductRequestDTO buildUpdateRequest(
            MultipartFile productFile,
            MultipartFile thumbnailFile
    ) {

        return UpdateProductRequestDTO.builder()
                .title("Updated title")
                .description("Updated description")
                .price(new BigDecimal("125.00"))
                .category("Updated guides")
                .productFile(productFile)
                .thumbnailFile(thumbnailFile)
                .build();
    }
}
