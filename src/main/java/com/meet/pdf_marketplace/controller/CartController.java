package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.cart.AddCartItemRequestDTO;
import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.cart.CartResponseDTO;
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
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    private final CurrentUserService currentUserService;

    /**
     * Gets the active cart for a user.
     * Creates an empty active cart when one does not exist.
     */
    @GetMapping("/me")
    public ApiResponseDTO<CartResponseDTO> getMe() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CartResponseDTO cart = cartService.getOrCreateActiveCart(currentUser);

        return ApiResponseDTO.<CartResponseDTO>builder()
                .success(true)
                .message("Cart fetched successfully")
                .data(cart)
                .build();
    }

    /**
     * Adds a product to the user's active cart.
     * Returns the updated cart with recalculated total.
     */
    @PostMapping("/items")
    public ApiResponseDTO<CartResponseDTO> addItem(
            @Valid @RequestBody AddCartItemRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CartResponseDTO cart = cartService.addItem(currentUser, request);

        return ApiResponseDTO.<CartResponseDTO>builder()
                .success(true)
                .message("Cart item added successfully")
                .data(cart)
                .build();
    }

    /**
     * Removes a cart item by id.
     * Returns the updated cart with recalculated total.
     */
    @DeleteMapping("/items/{cartItemId}")
    public ApiResponseDTO<CartResponseDTO> removeItem(
            @PathVariable UUID cartItemId
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CartResponseDTO cart = cartService.removeItem(currentUser, cartItemId);

        return ApiResponseDTO.<CartResponseDTO>builder()
                .success(true)
                .message("Cart item removed successfully")
                .data(cart)
                .build();
    }

    /**
     * Clears all items from the user's active cart.
     * Returns the empty cart with total reset to zero.
     */
    @DeleteMapping("/clear")
    public ApiResponseDTO<CartResponseDTO> clear() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CartResponseDTO cart = cartService.clear(currentUser);

        return ApiResponseDTO.<CartResponseDTO>builder()
                .success(true)
                .message("Cart cleared successfully")
                .data(cart)
                .build();
    }
}

