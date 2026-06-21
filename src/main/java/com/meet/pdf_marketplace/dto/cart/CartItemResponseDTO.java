package com.meet.pdf_marketplace.dto.cart;

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
public class CartItemResponseDTO {

    private UUID id;

    private UUID productId;

    private String productTitle;

    private String fileType;

    private String thumbnailKey;

    private BigDecimal currentPrice;

    private BigDecimal priceAtTime;
}
