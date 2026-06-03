package com.meet.pdf_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPaymentDetailsRequestDTO {

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    private String bankName;

    private String accountNumber;

    private String ifscCode;

    private String upiId;
}
