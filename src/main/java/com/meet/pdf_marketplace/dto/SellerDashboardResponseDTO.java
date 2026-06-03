package com.meet.pdf_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardResponseDTO {

    private Long paidOrderCount;

    private Long soldItemCount;

    private BigDecimal totalSalesAmount;

    private BigDecimal totalSellerEarning;

    private BigDecimal totalPlatformFee;
}
