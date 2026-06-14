package com.meet.pdf_marketplace.dto.seller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPaymentDetailsResponseDTO {

    private UUID id;

    private UUID sellerId;

    private String accountHolderName;

    private String bankName;

    private String maskedAccountNumber;

    private String ifscCode;

    private String upiId;
}

