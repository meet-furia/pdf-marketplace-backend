package com.meet.pdf_marketplace.controller.customer;

import com.meet.pdf_marketplace.dto.cart.AddCartItemRequestDTO;
import com.meet.pdf_marketplace.dto.cart.CartResponseDTO;
import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CartService;
import com.meet.pdf_marketplace.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/customer/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    /**
     * Gets the current customer's active cart.
     */
    @GetMapping
    public ApiResponseDTO<CartResponseDTO> getMyCart() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CartResponseDTO cart = cartService.getOrCreateMyActiveCart(currentUser);

        return ApiResponseDTO.<CartResponseDTO>builder()
                .success(true)
                .message("Cart fetched successfully")
                .data(cart)
                .build();
    }

    /**
     * Adds a published product to the current customer's cart.
     */
    @PostMapping("/items")
    public ApiResponseDTO<CartResponseDTO> addItemToCart(
            @Valid @RequestBody AddCartItemRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CartResponseDTO cart = cartService.addProductToCart(currentUser, request);

        return ApiResponseDTO.<CartResponseDTO>builder()
                .success(true)
                .message("Cart item added successfully")
                .data(cart)
                .build();
    }

    /**
     * Removes one cart item from the current customer's cart.
     */
    @DeleteMapping("/items/{cartItemId}")
    public ApiResponseDTO<CartResponseDTO> removeItemFromCart(
            @PathVariable UUID cartItemId
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CartResponseDTO cart = cartService.removeItemFromCart(currentUser, cartItemId);

        return ApiResponseDTO.<CartResponseDTO>builder()
                .success(true)
                .message("Cart item removed successfully")
                .data(cart)
                .build();
    }

    /**
     * Clears all items from the current customer's active cart.
     */
    @DeleteMapping("/items")
    public ApiResponseDTO<CartResponseDTO> clearMyCart() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CartResponseDTO cart = cartService.clearCart(currentUser);

        return ApiResponseDTO.<CartResponseDTO>builder()
                .success(true)
                .message("Cart cleared successfully")
                .data(cart)
                .build();
    }
}
