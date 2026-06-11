package com.meet.pdf_marketplace.dto.order;

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
public class OrderItemResponseDTO {

    private UUID id;

    private UUID productId;

    private String productTitle;

    private String thumbnailKey;

    private BigDecimal priceAtPurchase;

    private BigDecimal platformFee;

    private BigDecimal sellerEarning;
}

