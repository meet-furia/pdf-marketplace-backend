package com.meet.pdf_marketplace.controller.customer;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.order.OrderResponseDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CurrentUserService;
import com.meet.pdf_marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/customer/orders")
public class OrderController {

    private final OrderService orderService;

    private final CurrentUserService currentUserService;

    /**
     * Lists orders for the current customer.
     */
    @GetMapping
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
     * Gets one order owned by the current customer.
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

