package com.meet.pdf_marketplace.dto.product;

import com.meet.pdf_marketplace.enums.PdfProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private LocalDateTime createdAt;

    private String createdBy;

    private UUID id;

    private UUID sellerId;

    private String sellerName;

    private String title;

    private String description;

    private BigDecimal price;

    private String fileType;

    private String fileContentType;

    private String fileOriginalName;

    private Long fileSizeBytes;

    private String thumbnailKey;

    private String thumbnailUrl;

    private String category;

    private PdfProductStatus status;
}

