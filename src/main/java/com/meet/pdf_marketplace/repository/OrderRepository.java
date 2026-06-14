package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.OrderEntity;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends AbstractRepository<OrderEntity, UUID> {

    List<OrderEntity> findByUserId(UUID userId);
}

