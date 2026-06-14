package com.meet.pdf_marketplace.dto.payment;

import com.meet.pdf_marketplace.enums.PaymentStatus;
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
public class CreatePaymentResponseDTO {

    private UUID paymentId;

    private UUID orderId;

    private String keyId;

    private String razorpayOrderId;

    private BigDecimal amount;

    private String currency;

    private PaymentStatus status;
}

