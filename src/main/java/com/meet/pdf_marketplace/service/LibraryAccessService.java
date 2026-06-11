package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.entity.OrderEntity;
import com.meet.pdf_marketplace.entity.OrderItemEntity;
import com.meet.pdf_marketplace.entity.PurchasedPdfEntity;
import com.meet.pdf_marketplace.repository.OrderItemRepository;
import com.meet.pdf_marketplace.repository.PurchasedPdfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryAccessService {

    private final OrderItemRepository orderItemRepository;

    private final PurchasedPdfRepository purchasedPdfRepository;

    public void grantAccessForOrder(OrderEntity order) {

        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(order.getId());

        for (OrderItemEntity item : orderItems) {
            boolean exists = purchasedPdfRepository.existsByUserIdAndProductId(
                    order.getUser().getId(),
                    item.getProduct().getId()
            );

            if (!exists) {
                purchasedPdfRepository.save(PurchasedPdfEntity.builder()
                        .user(order.getUser())
                        .product(item.getProduct())
                        .order(order)
                        .accessGrantedAt(LocalDateTime.now(ZoneOffset.UTC))
                        .build());
            }
        }
    }
}

