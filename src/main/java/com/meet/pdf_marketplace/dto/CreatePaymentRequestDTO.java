package com.meet.pdf_marketplace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequestDTO {

    @NotNull(message = "Order id is required")
    private UUID orderId;
}
