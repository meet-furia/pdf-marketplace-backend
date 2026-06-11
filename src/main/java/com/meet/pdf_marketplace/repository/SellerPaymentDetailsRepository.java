package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.SellerPaymentDetailsEntity;

import java.util.Optional;
import java.util.UUID;

public interface SellerPaymentDetailsRepository extends AbstractRepository<SellerPaymentDetailsEntity, UUID> {

    Optional<SellerPaymentDetailsEntity> findBySellerId(UUID sellerId);
}

