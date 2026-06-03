package com.meet.pdf_marketplace.dto;

import com.meet.pdf_marketplace.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private UUID id;

    private UUID userId;

    private BigDecimal totalAmount;

    private BigDecimal platformFee;

    private BigDecimal sellerEarning;

    private String currency;

    private OrderStatus status;

    private LocalDateTime createdAt;

    private List<OrderItemResponseDTO> items;
}
