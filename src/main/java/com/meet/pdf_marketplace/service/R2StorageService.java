package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.config.R2Properties;
import com.meet.pdf_marketplace.dto.GenerateDownloadUrlResponseDTO;
import com.meet.pdf_marketplace.dto.GenerateUploadUrlRequestDTO;
import com.meet.pdf_marketplace.dto.GenerateUploadUrlResponseDTO;
import com.meet.pdf_marketplace.entity.PdfProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.PdfProductRepository;
import com.meet.pdf_marketplace.repository.PurchasedPdfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(10);

    private static final Duration DOWNLOAD_URL_DURATION = Duration.ofMinutes(5);

    private static final long MAX_PDF_SIZE = 50L * 1024L * 1024L;

    private static final long MAX_THUMBNAIL_SIZE = 5L * 1024L * 1024L;

    private final R2Properties r2Properties;

    private final PdfProductRepository pdfProductRepository;

    private final PurchasedPdfRepository purchasedPdfRepository;

    /**
     * Generates a presigned R2 upload URL for the current user.
     * Upload keys are scoped by file type and user id.
     */
    public GenerateUploadUrlResponseDTO generateUploadUrl(
            UserEntity currentUser,
            GenerateUploadUrlRequestDTO request
    ) {

        validateUploadRequest(request);

        String fileKey = buildFileKey(currentUser, request);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(fileKey)
                .contentType(request.getContentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_DURATION)
                .putObjectRequest(putObjectRequest)
                .build();

        return GenerateUploadUrlResponseDTO.builder()
                .uploadUrl(createPresigner().presignPutObject(presignRequest).url().toString())
                .fileKey(fileKey)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(UPLOAD_URL_DURATION))
                .build();
    }

    /**
     * Generates a short-lived R2 download URL after access is verified.
     * Sellers can download their own PDF and buyers need a purchase record.
     */
    @Transactional(readOnly = true)
    public GenerateDownloadUrlResponseDTO generateDownloadUrl(UserEntity currentUser, String fileKey) {

        PdfProductEntity product = pdfProductRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException("PDF file not found"));

        if (!hasAccess(currentUser, product)) {
            throw new IllegalArgumentException("Current user does not have access to this PDF");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_URL_DURATION)
                .getObjectRequest(getObjectRequest)
                .build();

        return GenerateDownloadUrlResponseDTO.builder()
                .downloadUrl(createPresigner().presignGetObject(presignRequest).url().toString())
                .fileKey(fileKey)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(DOWNLOAD_URL_DURATION))
                .build();
    }

    /**
     * Generates a short-lived R2 download URL for an already-authorized file key.
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

        return GenerateDownloadUrlResponseDTO.builder()
                .downloadUrl(createPresigner().presignGetObject(presignRequest).url().toString())
                .fileKey(fileKey)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(DOWNLOAD_URL_DURATION))
                .build();
    }

    private String buildFileKey(UserEntity currentUser, GenerateUploadUrlRequestDTO request) {

        String prefix = switch (normalizeFileType(request.getFileType())) {
            case "PDF" -> "products/pdfs/";
            case "THUMBNAIL" -> "products/thumbnails/";
            default -> throw new IllegalArgumentException("File type must be PDF or THUMBNAIL");
        };

        return prefix + currentUser.getId() + "/" + UUID.randomUUID() + "-" + sanitizeFileName(request.getFileName());
    }

    private String sanitizeFileName(String fileName) {

        return fileName.replace("\\", "/")
                .substring(fileName.replace("\\", "/").lastIndexOf("/") + 1)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void validateUploadRequest(GenerateUploadUrlRequestDTO request) {

        String fileType = normalizeFileType(request.getFileType());
        String contentType = request.getContentType().toLowerCase(Locale.ROOT);
        String fileName = request.getFileName().toLowerCase(Locale.ROOT);

        if ("PDF".equals(fileType)) {
            validatePdfUpload(fileName, contentType, request.getFileSize());
            return;
        }

        if ("THUMBNAIL".equals(fileType)) {
            validateThumbnailUpload(fileName, contentType, request.getFileSize());
            return;
        }

        throw new IllegalArgumentException("File type must be PDF or THUMBNAIL");
    }

    private void validatePdfUpload(String fileName, String contentType, Long fileSize) {

        if (!"application/pdf".equals(contentType)) {
            throw new IllegalArgumentException("PDF content type must be application/pdf");
        }

        if (!fileName.endsWith(".pdf")) {
            throw new IllegalArgumentException("PDF file name must end with .pdf");
        }

        if (fileSize > MAX_PDF_SIZE) {
            throw new IllegalArgumentException("PDF file size must be 50 MB or less");
        }
    }

    private void validateThumbnailUpload(String fileName, String contentType, Long fileSize) {

        boolean validContentType = "image/jpeg".equals(contentType)
                || "image/png".equals(contentType)
                || "image/webp".equals(contentType);

        boolean validExtension = fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".png")
                || fileName.endsWith(".webp");

        if (!validContentType) {
            throw new IllegalArgumentException("Thumbnail content type must be image/jpeg, image/png, or image/webp");
        }

        if (!validExtension) {
            throw new IllegalArgumentException("Thumbnail file name must end with .jpg, .jpeg, .png, or .webp");
        }

        if (fileSize > MAX_THUMBNAIL_SIZE) {
            throw new IllegalArgumentException("Thumbnail file size must be 5 MB or less");
        }
    }

    private String normalizeFileType(String fileType) {

        return fileType.toUpperCase(Locale.ROOT);
    }

    private boolean hasAccess(UserEntity currentUser, PdfProductEntity product) {

        if (product.getSeller().getId().equals(currentUser.getId())) {
            return true;
        }

        return purchasedPdfRepository.existsByUserIdAndProductId(currentUser.getId(), product.getId());
    }

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

    private void validateProperties() {

        if (isBlank(r2Properties.getEndpoint())
                || isBlank(r2Properties.getAccessKeyId())
                || isBlank(r2Properties.getSecretAccessKey())
                || isBlank(r2Properties.getBucketName())) {
            throw new IllegalArgumentException("Cloudflare R2 configuration is incomplete");
        }
    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();
    }
}
