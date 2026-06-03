package com.meet.pdf_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchasedPdfResponseDTO {

    private UUID id;

    private UUID userId;

    private UUID productId;

    private UUID orderId;

    private String title;

    private String description;

    private String thumbnailKey;

    private String category;

    private LocalDateTime accessGrantedAt;
}
