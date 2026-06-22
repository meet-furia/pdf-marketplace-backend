package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.RazorpayPaymentEntity;

import java.util.Optional;
import java.util.UUID;

public interface RazorpayPaymentRepository extends AbstractRepository<RazorpayPaymentEntity, UUID> {

    Optional<RazorpayPaymentEntity> findByPaymentId(UUID paymentId);

    Optional<RazorpayPaymentEntity> findByRazorpayOrderId(String razorpayOrderId);
}
