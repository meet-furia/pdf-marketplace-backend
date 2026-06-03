package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.OrderItemResponseDTO;
import com.meet.pdf_marketplace.dto.OrderResponseDTO;
import com.meet.pdf_marketplace.entity.CartEntity;
import com.meet.pdf_marketplace.entity.CartItemEntity;
import com.meet.pdf_marketplace.entity.OrderEntity;
import com.meet.pdf_marketplace.entity.OrderItemEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.CartStatus;
import com.meet.pdf_marketplace.enums.OrderStatus;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.CartItemRepository;
import com.meet.pdf_marketplace.repository.CartRepository;
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

    private static final String DEFAULT_CURRENCY = "INR";

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final CartRepository cartRepository;

    private final CartItemRepository cartItemRepository;

    /**
     * Creates a pending order from the current user's active cart.
     * Copies item prices and marks the cart as checked out.
     */
    @Transactional
    public OrderResponseDTO createFromCart(UserEntity currentUser) {

        CartEntity cart = cartRepository.findByUserIdAndStatus(currentUser.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

        List<CartItemEntity> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart must not be empty");
        }

        BigDecimal totalAmount = sumPrices(cartItems);
        BigDecimal platformFee = calculatePlatformFee(totalAmount);
        BigDecimal sellerEarning = totalAmount.subtract(platformFee);

        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .user(currentUser)
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .sellerEarning(sellerEarning)
                .currency(DEFAULT_CURRENCY)
                .status(OrderStatus.PENDING)
                .build());

        for (CartItemEntity cartItem : cartItems) {
            BigDecimal itemPlatformFee = calculatePlatformFee(cartItem.getPriceAtTime());

            orderItemRepository.save(OrderItemEntity.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .priceAtPurchase(cartItem.getPriceAtTime())
                    .platformFee(itemPlatformFee)
                    .sellerEarning(cartItem.getPriceAtTime().subtract(itemPlatformFee))
                    .build());
        }

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        return toResponse(order);
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
                .map(CartItemEntity::getPriceAtTime)
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
