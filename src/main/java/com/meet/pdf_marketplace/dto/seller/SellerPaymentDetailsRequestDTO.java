package com.meet.pdf_marketplace.dto.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    @Pattern(
            regexp = "^[\\p{L}\\p{M} .'-]+$",
            message = "Account holder name contains invalid characters"
    )
    private String accountHolderName;

    @Size(max = 100, message = "Bank name must be 100 characters or less")
    private String bankName;

    @Pattern(
            regexp = "^$|^[0-9]{9,18}$",
            message = "Account number must contain 9 to 18 digits"
    )
    private String accountNumber;

    @Pattern(
            regexp = "^$|^[A-Za-z]{4}0[A-Za-z0-9]{6}$",
            message = "IFSC code must use the format ABCD0123456"
    )
    private String ifscCode;

    @Pattern(
            regexp = "^$|^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9.-]{2,64}$",
            message = "UPI ID must use a valid format such as name@bank"
    )
    private String upiId;
}

