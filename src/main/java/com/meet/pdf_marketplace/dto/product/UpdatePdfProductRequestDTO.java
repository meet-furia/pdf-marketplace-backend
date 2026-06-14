package com.meet.pdf_marketplace.dto.product;

import com.meet.pdf_marketplace.enums.PdfProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePdfProductRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Product price must be positive")
    private BigDecimal price;

    @NotBlank(message = "File key is required")
    private String fileKey;

    private String thumbnailKey;

    private String category;

    @NotNull(message = "Status is required")
    private PdfProductStatus status;
}

