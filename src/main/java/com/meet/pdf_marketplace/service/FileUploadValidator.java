package com.meet.pdf_marketplace.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class FileUploadValidator {

    static final long MAX_PRODUCT_FILE_SIZE = 10L * 1024L * 1024L;

    static final long MAX_THUMBNAIL_FILE_SIZE = 10L * 1024L * 1024L;

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "3g2", "3gp", "asf", "avi", "flv", "m2ts", "m4v", "mkv", "mov",
            "mp4", "mpeg", "mpg", "mts", "ogv", "rm", "rmvb", "ts", "vob",
            "webm", "wmv"
    );

    public void validateProductFile(String fileName, String contentType, long fileSize) {

        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        String extension = getExtension(normalizedFileName);

        if (normalizedContentType.startsWith("video/") || VIDEO_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Video files are not supported yet");
        }

        if (fileSize > MAX_PRODUCT_FILE_SIZE) {
            throw new IllegalArgumentException("Product file size must be 10 MB or less");
        }
    }

    public void validateThumbnailFile(String fileName, String contentType, long fileSize) {

        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);

        boolean validContentType = "image/jpeg".equals(normalizedContentType)
                || "image/png".equals(normalizedContentType)
                || "image/webp".equals(normalizedContentType)
                || "image/gif".equals(normalizedContentType);

        boolean validExtension = normalizedFileName.endsWith(".jpg")
                || normalizedFileName.endsWith(".jpeg")
                || normalizedFileName.endsWith(".png")
                || normalizedFileName.endsWith(".webp")
                || normalizedFileName.endsWith(".gif");

        if (!validContentType) {
            throw new IllegalArgumentException("Image content type must be image/jpeg, image/png, image/webp, or image/gif");
        }

        if (!validExtension) {
            throw new IllegalArgumentException("Image file name must end with .jpg, .jpeg, .png, .webp, or .gif");
        }

        if (fileSize > MAX_THUMBNAIL_FILE_SIZE) {
            throw new IllegalArgumentException("Image file size must be 10 MB or less");
        }
    }

    public String determineProductFileType(String fileName) {

        String extension = getExtension(fileName);

        return extension.isBlank() ? "FILE" : extension.toUpperCase(Locale.ROOT);
    }

    private String getExtension(String fileName) {

        int extensionSeparator = fileName.lastIndexOf('.');

        if (extensionSeparator < 0 || extensionSeparator == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
    }
}
