package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.CartItemEntity;

import java.util.List;
import java.util.UUID;

public interface CartItemRepository extends AbstractRepository<CartItemEntity, UUID> {

    List<CartItemEntity> findByCartId(UUID cartId);

    boolean existsByCartIdAndProductId(UUID cartId, UUID productId);
}
