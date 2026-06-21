package com.meet.pdf_marketplace.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUploadValidatorTests {

    private final FileUploadValidator validator = new FileUploadValidator();

    @Test
    void acceptsCommonDigitalProductFormats() {

        assertDoesNotThrow(() -> validator.validateProductFile("guide.pdf", "application/pdf", 1024));
        assertDoesNotThrow(() -> validator.validateProductFile("artwork.png", "image/png", 1024));
        assertDoesNotThrow(() -> validator.validateProductFile("templates.zip", "application/zip", 1024));
        assertDoesNotThrow(() -> validator.validateProductFile("track.mp3", "audio/mpeg", 1024));
        assertDoesNotThrow(() -> validator.validateProductFile(
                "unknown.custom",
                "application/octet-stream",
                1024
        ));
    }

    @Test
    void rejectsVideoByContentTypeOrExtension() {

        IllegalArgumentException contentTypeError = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateProductFile("recording.bin", "video/mp4", 1024)
        );
        IllegalArgumentException extensionError = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateProductFile("recording.mkv", "application/octet-stream", 1024)
        );

        assertEquals("Video files are not supported yet", contentTypeError.getMessage());
        assertEquals("Video files are not supported yet", extensionError.getMessage());
    }

    @Test
    void rejectsOversizedProductFiles() {

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateProductFile(
                        "templates.zip",
                        "application/zip",
                        FileUploadValidator.MAX_PRODUCT_FILE_SIZE + 1
                )
        );

        assertEquals("Product file size must be 10 MB or less", error.getMessage());
    }

    @Test
    void derivesDisplayTypeFromExtension() {

        assertEquals("PDF", validator.determineProductFileType("guide.pdf"));
        assertEquals("ZIP", validator.determineProductFileType("templates.ZIP"));
        assertEquals("FILE", validator.determineProductFileType("README"));
    }
}
