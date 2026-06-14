package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.order.OrderResponseDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CurrentUserService;
import com.meet.pdf_marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    private final CurrentUserService currentUserService;

    /**
     * Disabled: orders are created only after successful payment verification.
     */
    @PostMapping("/from-cart")
    public ApiResponseDTO<OrderResponseDTO> createFromCart() {

        throw new IllegalArgumentException("Orders are created only after successful payment");
    }

    /**
     * Lists orders for the current user.
     * No user id is accepted from the frontend.
     */
    @GetMapping("/me")
    public ApiResponseDTO<List<OrderResponseDTO>> getMyOrders() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        List<OrderResponseDTO> orders = orderService.getMyOrders(currentUser);

        return ApiResponseDTO.<List<OrderResponseDTO>>builder()
                .success(true)
                .message("Orders fetched successfully")
                .data(orders)
                .build();
    }

    /**
     * Gets one order owned by the current user.
     * Users cannot fetch other users' orders.
     */
    @GetMapping("/{orderId}")
    public ApiResponseDTO<OrderResponseDTO> getById(
            @PathVariable UUID orderId
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        OrderResponseDTO order = orderService.getById(currentUser, orderId);

        return ApiResponseDTO.<OrderResponseDTO>builder()
                .success(true)
                .message("Order fetched successfully")
                .data(order)
                .build();
    }
}

