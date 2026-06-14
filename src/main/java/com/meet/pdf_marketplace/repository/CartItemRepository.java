package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.CartElementEntity;

import java.util.List;
import java.util.UUID;

public interface CartItemRepository extends AbstractRepository<CartElementEntity, UUID> {

    List<CartElementEntity> findByCartId(UUID cartId);

    boolean existsByCartIdAndProductId(UUID cartId, UUID productId);
}

