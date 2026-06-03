package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.PurchasedPdfEntity;

import java.util.List;
import java.util.UUID;

public interface PurchasedPdfRepository extends AbstractRepository<PurchasedPdfEntity, UUID> {

    List<PurchasedPdfEntity> findByUserId(UUID userId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
