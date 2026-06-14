package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.PaymentEntity;
import com.meet.pdf_marketplace.enums.PaymentStatus;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends AbstractRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByCartIdAndStatus(UUID cartId, PaymentStatus status);

    Optional<PaymentEntity> findByRazorpayOrderId(String razorpayOrderId);
}

