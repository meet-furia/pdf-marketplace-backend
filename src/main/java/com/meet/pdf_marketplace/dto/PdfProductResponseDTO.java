package com.meet.pdf_marketplace.dto;

import com.meet.pdf_marketplace.enums.PdfProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfProductResponseDTO {

    private UUID id;

    private UUID sellerId;

    private String sellerName;

    private String title;

    private String description;

    private BigDecimal price;

    private String fileKey;

    private String thumbnailKey;

    private String category;

    private PdfProductStatus status;
}
