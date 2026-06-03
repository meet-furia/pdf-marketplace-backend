package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.OrderResponseDTO;
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
     * Creates a pending order from the current user's active cart.
     * The current user comes from the Supabase JWT.
     */
    @PostMapping("/from-cart")
    public ApiResponseDTO<OrderResponseDTO> createFromCart() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        OrderResponseDTO order = orderService.createFromCart(currentUser);

        return ApiResponseDTO.<OrderResponseDTO>builder()
                .success(true)
                .message("Order created successfully")
                .data(order)
                .build();
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
