package com.meet.pdf_marketplace.dto.cart;

import com.meet.pdf_marketplace.enums.CartStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDTO {

    private UUID id;

    private UUID userId;

    private CartStatus status;

    private BigDecimal totalAmount;

    private List<CartItemResponseDTO> items;
}

