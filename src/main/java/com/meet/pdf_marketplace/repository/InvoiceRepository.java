package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.InvoiceEntity;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends AbstractRepository<InvoiceEntity, UUID> {

    Optional<InvoiceEntity> findByOrderId(UUID orderId);

    Optional<InvoiceEntity> findByPaymentId(UUID paymentId);
}

