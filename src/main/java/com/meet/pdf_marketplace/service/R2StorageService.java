package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.config.R2Properties;
import com.meet.pdf_marketplace.dto.file.GenerateDownloadUrlResponseDTO;
import com.meet.pdf_marketplace.entity.ProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.ProductRepository;
import com.meet.pdf_marketplace.repository.PurchasedPdfRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService {

    private static final Duration DOWNLOAD_URL_DURATION = Duration.ofMinutes(5);

    private static final String PRODUCT_FILE_TYPE = "PRODUCT";

    private static final String THUMBNAIL_FILE_TYPE = "THUMBNAIL";

    private final R2Properties r2Properties;

    private final ProductRepository productRepository;

    private final PurchasedPdfRepository purchasedPdfRepository;

    private final FileUploadValidator fileUploadValidator;

    /**
     * Uploads the main digital product file to private Cloudflare R2 storage.
     */
    public StoredFile uploadProductFile(UserEntity currentUser, MultipartFile file) {

        UploadFileDetails fileDetails = validateAndBuildFileDetails(file, PRODUCT_FILE_TYPE);

        return uploadFile(currentUser, file, fileDetails);
    }

    /**
     * Uploads the optional product thumbnail image to private Cloudflare R2 storage.
     */
    public StoredFile uploadThumbnailFile(UserEntity currentUser, MultipartFile file) {

        UploadFileDetails fileDetails = validateAndBuildFileDetails(file, THUMBNAIL_FILE_TYPE);

        return uploadFile(currentUser, file, fileDetails);
    }

    /**
     * Extracts upload metadata and applies validation for the requested file role.
     */
    private UploadFileDetails validateAndBuildFileDetails(MultipartFile file, String requestedFileType) {

        UploadFileDetails fileDetails = buildFileDetails(file, requestedFileType);

        validateBasicUploadDetails(fileDetails);

        if (PRODUCT_FILE_TYPE.equals(requestedFileType)) {
            fileUploadValidator.validateProductFile(
                    fileDetails.fileName(),
                    fileDetails.contentType(),
                    fileDetails.fileSize()
            );

            return new UploadFileDetails(
                    fileDetails.fileName(),
                    fileDetails.contentType(),
                    fileUploadValidator.determineProductFileType(fileDetails.fileName()),
                    fileDetails.fileSize()
            );
        }

        if (THUMBNAIL_FILE_TYPE.equals(requestedFileType)) {
            fileUploadValidator.validateThumbnailFile(
                    fileDetails.fileName(),
                    fileDetails.contentType(),
                    fileDetails.fileSize()
            );
            return fileDetails;
        }

        throw new IllegalArgumentException("Unsupported upload file type");
    }

    /**
     * Builds the initial metadata representation from a multipart upload.
     */
    private UploadFileDetails buildFileDetails(MultipartFile file, String requestedFileType) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        return new UploadFileDetails(
                file.getOriginalFilename(),
                file.getContentType(),
                requestedFileType,
                file.getSize()
        );
    }

    /**
     * Streams a validated file to R2 and returns its stored metadata.
     */
    private StoredFile uploadFile(UserEntity currentUser, MultipartFile file, UploadFileDetails fileDetails) {

        String fileKey = buildFileKey(currentUser, fileDetails);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(fileKey)
                .contentType(fileDetails.contentType())
                // Make purchased files download with a useful, sanitized original name.
                .contentDisposition("attachment; filename=\"" + sanitizeFileName(fileDetails.fileName()) + "\"")
                .contentLength(fileDetails.fileSize())
                .build();

        log.info("Uploading file to R2. userId={}, fileKey={}", currentUser.getId(), fileKey);

        try (S3Client s3Client = createS3Client()) {
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), fileDetails.fileSize())
            );
        } catch (IOException exception) {
            log.error("Unable to read uploaded file. userId={}", currentUser.getId(), exception);
            throw new IllegalArgumentException("Unable to read uploaded file");
        } catch (S3Exception exception) {
            log.error("Failed to upload file to R2. userId={}, fileKey={}", currentUser.getId(), fileKey, exception);
            throw new IllegalArgumentException("Failed to upload file to storage");
        }

        log.info("File uploaded successfully to R2. userId={}, fileKey={}", currentUser.getId(), fileKey);

        return new StoredFile(
                fileKey,
                fileDetails.fileType(),
                fileDetails.contentType(),
                fileDetails.fileName(),
                fileDetails.fileSize()
        );
    }

    /**
     * Deletes a file from R2.
     */
    public void deleteFile(String fileKey) {

        if (isBlank(fileKey)) {
            return;
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(fileKey)
                .build();

        log.info("Deleting file from R2. fileKey={}", fileKey);

        try (S3Client s3Client = createS3Client()) {
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception exception) {
            log.error("Failed to delete file from R2. fileKey={}", fileKey, exception);
            throw new IllegalArgumentException("Failed to delete file from storage");
        }
    }

    /**
     * Generates a short-lived download URL after checking seller or buyer access.
     */
    @Transactional(readOnly = true)
    public GenerateDownloadUrlResponseDTO generateDownloadUrl(UserEntity currentUser, String fileKey) {

        ProductEntity product = productRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException("Product file not found"));

        if (!hasAccess(currentUser, product)) {
            throw new IllegalArgumentException("Current user does not have access to this product file");
        }

        return generateAuthorizedDownloadUrl(fileKey);
    }

    /**
     * Generates a short-lived download URL for an already-authorized file key.
     */
    public GenerateDownloadUrlResponseDTO generateAuthorizedDownloadUrl(String fileKey) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_URL_DURATION)
                .getObjectRequest(getObjectRequest)
                .build();

        String downloadUrl = createPresigner()
                .presignGetObject(presignRequest)
                .url()
                .toString();

        log.info("Generated download URL for fileKey={}", fileKey);

        return GenerateDownloadUrlResponseDTO.builder()
                .downloadUrl(downloadUrl)
                .fileKey(fileKey)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(DOWNLOAD_URL_DURATION))
                .build();
    }

    /**
     * Builds a browser-accessible URL for a stored public-facing asset.
     */
    public String generateAssetUrl(String fileKey) {

        if (isBlank(fileKey)) {
            return null;
        }

        if (!isBlank(r2Properties.getPublicBaseUrl())) {
            return r2Properties.getPublicBaseUrl().replaceAll("/+$", "") + "/" + fileKey;
        }

        // Local/private configurations may not expose a public R2 domain, so use a
        // temporary signed URL for thumbnails as a fallback.
        return generateAuthorizedDownloadUrl(fileKey).getDownloadUrl();
    }

    /**
     * Creates a unique, seller-scoped R2 key for a product file or thumbnail.
     */
    private String buildFileKey(UserEntity currentUser, UploadFileDetails fileDetails) {

        String prefix = switch (fileDetails.fileType()) {
            case THUMBNAIL_FILE_TYPE -> "products/thumbnails/";
            default -> "products/files/";
        };

        return prefix + currentUser.getId() + "/" + UUID.randomUUID() + "-" + sanitizeFileName(fileDetails.fileName());
    }

    /**
     * Removes client paths and unsafe characters from an uploaded filename.
     */
    private String sanitizeFileName(String fileName) {

        // Strip any client-supplied path and keep only storage/header-safe characters.
        return fileName.replace("\\", "/")
                .substring(fileName.replace("\\", "/").lastIndexOf("/") + 1)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Verifies that required filename, content type, and size metadata are present.
     */
    private void validateBasicUploadDetails(UploadFileDetails fileDetails) {

        if (isBlank(fileDetails.fileName())) {
            throw new IllegalArgumentException("File name is required");
        }

        if (isBlank(fileDetails.contentType())) {
            throw new IllegalArgumentException("Content type is required");
        }

        if (fileDetails.fileSize() == null || fileDetails.fileSize() <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
    }

    /**
     * Checks whether the user owns or has purchased the requested product.
     */
    private boolean hasAccess(UserEntity currentUser, ProductEntity product) {

        if (product.getSeller().getId().equals(currentUser.getId())) {
            return true;
        }

        return purchasedPdfRepository.existsByUserIdAndProductId(currentUser.getId(), product.getId());
    }

    /**
     * Creates an R2 presigner used to generate short-lived download URLs.
     */
    private S3Presigner createPresigner() {

        validateProperties();

        return S3Presigner.builder()
                .endpointOverride(URI.create(r2Properties.getEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        r2Properties.getAccessKeyId(),
                        r2Properties.getSecretAccessKey()
                )))
                .build();
    }

    /**
     * Creates an authenticated R2 client used for object uploads and deletions.
     */
    private S3Client createS3Client() {

        validateProperties();

        return S3Client.builder()
                .endpointOverride(URI.create(r2Properties.getEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        r2Properties.getAccessKeyId(),
                        r2Properties.getSecretAccessKey()
                )))
                .build();
    }

    /**
     * Ensures all required R2 connection properties are configured.
     */
    private void validateProperties() {

        if (isBlank(r2Properties.getEndpoint())
                || isBlank(r2Properties.getAccessKeyId())
                || isBlank(r2Properties.getSecretAccessKey())
                || isBlank(r2Properties.getBucketName())) {
            throw new IllegalArgumentException("Cloudflare R2 configuration is incomplete");
        }
    }

    /**
     * Checks whether a string is null, empty, or whitespace-only.
     */
    private boolean isBlank(String value) {

        return value == null || value.isBlank();
    }

    private record UploadFileDetails(
            String fileName,
            String contentType,
            String fileType,
            Long fileSize
    ) {
    }

    public record StoredFile(
            String fileKey,
            String fileType,
            String contentType,
            String originalName,
            Long sizeBytes
    ) {
    }
}
