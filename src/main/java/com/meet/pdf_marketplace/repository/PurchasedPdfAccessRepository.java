package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.PurchasedPdfAccessEntity;

import java.util.UUID;

public interface PurchasedPdfAccessRepository extends AbstractRepository<PurchasedPdfAccessEntity, UUID> {

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
