package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.PaymentInvoiceEntity;

import java.util.Optional;
import java.util.UUID;

public interface PaymentInvoiceRepository extends AbstractRepository<PaymentInvoiceEntity, UUID> {

    Optional<PaymentInvoiceEntity> findByOrderId(UUID orderId);

    Optional<PaymentInvoiceEntity> findByRazorpayOrderId(String razorpayOrderId);
}

