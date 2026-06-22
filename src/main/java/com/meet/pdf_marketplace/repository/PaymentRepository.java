package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.PaymentEntity;
import com.meet.pdf_marketplace.enums.PaymentStatus;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends AbstractRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);

}

