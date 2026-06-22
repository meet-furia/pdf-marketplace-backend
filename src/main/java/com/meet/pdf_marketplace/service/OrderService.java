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
     * Creates a payment-pending order and snapshots the latest product prices.
     * Repeated checkout calls reuse the pending order for the same cart.
     */
    public OrderEntity createPaymentPendingOrder(
            UserEntity currentUser,
            CartEntity cart,
            String currency
    ) {

        return orderRepository.findByCartId(cart.getId())
                .orElseGet(() -> createOrder(currentUser, cart, currency));
    }

    /**
     * Creates the pending order and immutable order-item price snapshots.
     */
    private OrderEntity createOrder(UserEntity currentUser, CartEntity cart, String currency) {

        List<CartItemEntity> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart must not be empty");
        }

        BigDecimal totalAmount = sumPrices(cartItems);
        BigDecimal platformFee = calculatePlatformFee(totalAmount);

        // The order total is fixed before the external payment order is created.
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .user(currentUser)
                .cart(cart)
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .sellerEarning(totalAmount.subtract(platformFee))
                .currency(currency)
                .status(OrderStatus.PAYMENT_PENDING)
                .build());

        for (CartItemEntity cartItem : cartItems) {
            // Order items preserve the latest product price at checkout permanently.
            BigDecimal priceAtPurchase = cartItem.getProduct().getPrice();
            BigDecimal itemPlatformFee = calculatePlatformFee(priceAtPurchase);

            orderItemRepository.save(OrderItemEntity.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .priceAtPurchase(priceAtPurchase)
                    .platformFee(itemPlatformFee)
                    .sellerEarning(priceAtPurchase.subtract(itemPlatformFee))
                    .build());
        }

        return order;
    }

    /**
     * Completes the existing order after payment verification succeeds.
     */
    public OrderEntity completePayment(OrderEntity order) {

        order.setStatus(OrderStatus.COMPLETED);

        return orderRepository.save(order);
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
    private BigDecimal sumPrices(List<CartItemEntity> cartItems) {

        return cartItems.stream()
                .map(cartItem -> cartItem.getProduct().getPrice())
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
                .cartId(order.getCart().getId())
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
