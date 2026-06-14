package com.meet.pdf_marketplace.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    @Builder.Default
    private boolean success = false;

    private String error;

    private String message;

    private Integer status;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private List<FieldValidationError> validationErrors;

    @Getter
    @Setter
    @Builder
    public static class FieldValidationError {

        private String field;

        private String message;
    }
}

