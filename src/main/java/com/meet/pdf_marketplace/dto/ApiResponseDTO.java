package com.meet.pdf_marketplace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDTO<T> {

    @Builder.Default
    private boolean success = true;

    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private Pagination pagination;

    private T data;

    @Getter
    @Setter
    @Builder
    public static class Pagination {

        private Integer size;

        private Integer currentPage;

        private Integer totalPages;

        private Integer nextPage;

        private Integer previousPage;

        private Long totalElements;
    }
}