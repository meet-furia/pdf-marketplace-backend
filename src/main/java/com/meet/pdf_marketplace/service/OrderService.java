package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.order.OrderItemResponseDTO;
import com.meet.pdf_marketplace.dto.order.OrderResponseDTO;
import com.meet.pdf_marketplace.entity.*;
import com.meet.pdf_marketplace.enums.OrderStatus;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.CartItemRepository;
import com.meet.pdf_marketplace.repository.OrderItemRepository;
import com.meet.pdf_marketplace.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10");

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final CartItemRepository cartItemRepository;

    /**
     * Orders are created only by checkout after payment verification succeeds.
     */
    public OrderResponseDTO createFromCart(UserEntity currentUser) {

        throw new IllegalArgumentException("Orders are created only after successful payment");
    }

    /**
     * Creates an order from a paid payment and its frozen cart.
     * Duplicate calls return the already-created order.
     */
    public OrderEntity createFromPaidPayment(PaymentEntity payment) {

        if (payment.getOrder() != null) {
            return payment.getOrder();
        }

        List<CartElementEntity> cartItems = cartItemRepository.findByCartId(payment.getCart().getId());

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart must not be empty");
        }

        BigDecimal totalAmount = sumPrices(cartItems);
        BigDecimal platformFee = calculatePlatformFee(totalAmount);

        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .user(payment.getUser())
                .payment(payment)
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .sellerEarning(totalAmount.subtract(platformFee))
                .currency(payment.getCurrency())
                .status(OrderStatus.PAID)
                .build());

        for (CartElementEntity cartItem : cartItems) {
            BigDecimal itemPlatformFee = calculatePlatformFee(cartItem.getPriceAtTime());

            orderItemRepository.save(OrderItemEntity.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .priceAtPurchase(cartItem.getPriceAtTime())
                    .platformFee(itemPlatformFee)
                    .sellerEarning(cartItem.getPriceAtTime().subtract(itemPlatformFee))
                    .build());
        }

        payment.setOrder(order);

        return order;
    }

    /**
     * Lists orders that belong to the current user.
     * Each response includes the order items.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(UserEntity currentUser) {

        return orderRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Gets one order owned by the current user.
     * Users cannot fetch orders they do not own.
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getById(UserEntity currentUser, UUID orderId) {

        OrderEntity order = findOrder(orderId);

        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Current user does not own this order");
        }

        return toResponse(order);
    }

    /**
     * Loads an order or fails when the order does not exist.
     */
    private OrderEntity findOrder(UUID orderId) {

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    /**
     * Calculates the platform fee for an amount.
     */
    private BigDecimal calculatePlatformFee(BigDecimal amount) {

        return amount.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Sums cart item prices to get the order total.
     */
    private BigDecimal sumPrices(List<CartElementEntity> cartItems) {

        return cartItems.stream()
                .map(CartElementEntity::getPriceAtTime)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Converts an order entity into the response DTO.
     */
    private OrderResponseDTO toResponse(OrderEntity order) {

        List<OrderItemResponseDTO> items = orderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();

        return OrderResponseDTO.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .totalAmount(order.getTotalAmount())
                .platformFee(order.getPlatformFee())
                .sellerEarning(order.getSellerEarning())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    /**
     * Converts an order item entity into the response DTO.
     */
    private OrderItemResponseDTO toItemResponse(OrderItemEntity item) {

        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productTitle(item.getProduct().getTitle())
                .thumbnailKey(item.getProduct().getThumbnailKey())
                .priceAtPurchase(item.getPriceAtPurchase())
                .platformFee(item.getPlatformFee())
                .sellerEarning(item.getSellerEarning())
                .build();
    }
}

