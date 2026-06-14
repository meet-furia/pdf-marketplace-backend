package com.meet.pdf_marketplace.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Utils {

    private Utils() {
    }

    public static Pageable validateAndCreatePageable(
            final int page,
            final int pageSize,
            final String sortField,
            final String sortDirection
    ) {

        final int validPage = Math.max(page, 0);
        final int validPageSize = Math.min(Math.max(pageSize, 1), 100);
        final Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection)
                .orElse(Sort.Direction.DESC);

        return PageRequest.of(validPage, validPageSize, Sort.by(direction, sortField));
    }
}

