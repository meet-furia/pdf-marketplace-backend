package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.AddCartItemRequestDTO;
import com.meet.pdf_marketplace.dto.CartItemResponseDTO;
import com.meet.pdf_marketplace.dto.CartResponseDTO;
import com.meet.pdf_marketplace.entity.CartEntity;
import com.meet.pdf_marketplace.entity.CartItemEntity;
import com.meet.pdf_marketplace.entity.PdfProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.CartStatus;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.CartItemRepository;
import com.meet.pdf_marketplace.repository.CartRepository;
import com.meet.pdf_marketplace.repository.PdfProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    private final CartItemRepository cartItemRepository;

    private final PdfProductRepository pdfProductRepository;

    /**
     * Gets the current user's active cart or creates one when missing.
     * Returns the cart with its current items and total.
     */
    @Transactional
    public CartResponseDTO getOrCreateActiveCart(UserEntity currentUser) {

        CartEntity cart = getOrCreateCart(currentUser);

        return toResponse(cart);
    }

    /**
     * Adds a product to the current user's active cart.
     * Stores the product price at the time it is added.
     */
    @Transactional
    public CartResponseDTO addItem(UserEntity currentUser, AddCartItemRequestDTO request) {

        CartEntity cart = getOrCreateCart(currentUser);
        PdfProductEntity product = findProduct(request.getProductId());

        if (product.getSeller().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Buyer cannot add own product to cart");
        }

        if (cartItemRepository.existsByCartIdAndProductId(cart.getId(), product.getId())) {
            throw new IllegalArgumentException("Product already exists in active cart");
        }

        CartItemEntity item = CartItemEntity.builder()
                .cart(cart)
                .product(product)
                .priceAtTime(product.getPrice())
                .build();

        cartItemRepository.save(item);

        return toResponse(recalculateTotal(cart));
    }

    /**
     * Removes one item from a cart.
     * Recalculates the cart total after removal.
     */
    @Transactional
    public CartResponseDTO removeItem(UserEntity currentUser, UUID cartItemId) {

        CartItemEntity item = findCartItem(cartItemId);
        CartEntity cart = item.getCart();

        if (!cart.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Current user does not own this cart item");
        }

        cartItemRepository.delete(item);
        cartItemRepository.flush();

        return toResponse(recalculateTotal(cart));
    }

    /**
     * Clears all items from the current user's active cart.
     * Resets the cart total to zero.
     */
    @Transactional
    public CartResponseDTO clear(UserEntity currentUser) {

        CartEntity cart = getOrCreateCart(currentUser);
        List<CartItemEntity> items = cartItemRepository.findByCartId(cart.getId());

        cartItemRepository.deleteAll(items);
        cartItemRepository.flush();

        return toResponse(recalculateTotal(cart));
    }

    /**
     * Gets the active cart for a user or creates a new empty one.
     */
    private CartEntity getOrCreateCart(UserEntity currentUser) {

        return cartRepository.findByUserIdAndStatus(currentUser.getId(), CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(CartEntity.builder()
                        .user(currentUser)
                        .status(CartStatus.ACTIVE)
                        .totalAmount(BigDecimal.ZERO)
                        .build()));
    }

    /**
     * Loads a product or fails when the product does not exist.
     */
    private PdfProductEntity findProduct(UUID productId) {

        return pdfProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /**
     * Loads a cart item or fails when the cart item does not exist.
     */
    private CartItemEntity findCartItem(UUID cartItemId) {

        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    }

    /**
     * Recalculates and saves the cart total from current item prices.
     */
    private CartEntity recalculateTotal(CartEntity cart) {

        BigDecimal totalAmount = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(CartItemEntity::getPriceAtTime)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(totalAmount);

        return cartRepository.save(cart);
    }

    /**
     * Converts a cart entity into a response DTO.
     */
    private CartResponseDTO toResponse(CartEntity cart) {

        List<CartItemResponseDTO> items = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();

        return CartResponseDTO.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .status(cart.getStatus())
                .totalAmount(cart.getTotalAmount())
                .items(items)
                .build();
    }

    /**
     * Converts a cart item entity into a response DTO.
     */
    private CartItemResponseDTO toItemResponse(CartItemEntity item) {

        return CartItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productTitle(item.getProduct().getTitle())
                .thumbnailKey(item.getProduct().getThumbnailKey())
                .priceAtTime(item.getPriceAtTime())
                .build();
    }
}
