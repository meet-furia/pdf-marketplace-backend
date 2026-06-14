package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.CartEntity;
import com.meet.pdf_marketplace.enums.CartStatus;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends AbstractRepository<CartEntity, UUID> {

    Optional<CartEntity> findByUserIdAndStatus(UUID userId, CartStatus status);
}

