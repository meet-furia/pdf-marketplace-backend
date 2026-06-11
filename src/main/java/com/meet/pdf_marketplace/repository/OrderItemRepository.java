package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.OrderItemEntity;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends AbstractRepository<OrderItemEntity, UUID> {

    List<OrderItemEntity> findByOrderId(UUID orderId);

    List<OrderItemEntity> findByProductSellerId(UUID sellerId);
}

