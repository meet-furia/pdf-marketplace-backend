package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.cart.AddCartItemRequestDTO;
import com.meet.pdf_marketplace.dto.cart.CartItemResponseDTO;
import com.meet.pdf_marketplace.dto.cart.CartResponseDTO;
import com.meet.pdf_marketplace.entity.CartEntity;
import com.meet.pdf_marketplace.entity.CartItemEntity;
import com.meet.pdf_marketplace.entity.ProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.CartStatus;
import com.meet.pdf_marketplace.enums.PdfProductStatus;
import com.meet.pdf_marketplace.exception.BadRequestException;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.CartItemRepository;
import com.meet.pdf_marketplace.repository.CartRepository;
import com.meet.pdf_marketplace.repository.ProductRepository;
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
    private final ProductRepository productRepository;

    /**
     * Gets the current user's active cart or creates one when missing.
     * Returns the cart using latest product prices.
     */
    @Transactional
    public CartResponseDTO getOrCreateMyActiveCart(UserEntity currentUser) {

        CartEntity cart = getOrCreateActiveCartEntity(currentUser);

        return toResponse(recalculateTotal(cart));
    }

    /**
     * Adds a published product to the current user's active cart.
     * Cart stores product reference only; price is read live from product.
     */
    @Transactional
    public CartResponseDTO addProductToCart(UserEntity currentUser, AddCartItemRequestDTO request) {

        CartEntity cart = getOrCreateActiveCartEntity(currentUser);
        ProductEntity product = findProductById(request.getProductId());

        validateProductCanBeAddedToCart(product, currentUser);

        if (cartItemRepository.existsByCartIdAndProductId(cart.getId(), product.getId())) {
            throw new BadRequestException("Product already exists in active cart");
        }

        CartItemEntity item = CartItemEntity.builder()
                .cart(cart)
                .product(product)
                .build();

        cartItemRepository.save(item);

        return toResponse(recalculateTotal(cart));
    }

    /**
     * Freezes the active cart while a payment is in progress.
     * Existing pending cart is reused to keep payment creation idempotent.
     */
    public CartEntity getOrCreatePaymentPendingCart(UserEntity currentUser) {

        return cartRepository.findByUserIdAndStatus(currentUser.getId(), CartStatus.PAYMENT_PENDING)
                .orElseGet(() -> {
                    CartEntity cart = cartRepository.findByUserIdAndStatus(currentUser.getId(), CartStatus.ACTIVE)
                            .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

                    recalculateTotal(cart);
                    requireCartTotal(cart);
                    cart.setStatus(CartStatus.PAYMENT_PENDING);

                    return cartRepository.save(cart);
                });
    }

    /**
     * Returns a non-zero cart total or fails when the cart is empty.
     */
    public BigDecimal requireCartTotal(CartEntity cart) {

        if (cart.getTotalAmount() == null || cart.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Cart must not be empty");
        }

        return cart.getTotalAmount();
    }

    /**
     * Releases a failed pending-payment cart back to active.
     */
    public CartEntity releasePaymentPendingCart(CartEntity cart) {

        if (cart.getStatus() == CartStatus.PAYMENT_PENDING) {
            cart.setStatus(CartStatus.ACTIVE);
        }

        return cartRepository.save(cart);
    }

    /**
     * Marks a successfully paid cart as checked out.
     */
    public CartEntity checkoutCart(CartEntity cart) {

        cart.setStatus(CartStatus.CHECKED_OUT);

        return cartRepository.save(cart);
    }

    /**
     * Removes one cart item owned by the current user.
     * Recalculates the cart total after removal.
     */
    @Transactional
    public CartResponseDTO removeItemFromCart(UserEntity currentUser, UUID cartItemId) {

        CartItemEntity item = findCartItemById(cartItemId);
        CartEntity cart = item.getCart();

        if (!cart.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Current user does not own this cart item");
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
    public CartResponseDTO clearCart(UserEntity currentUser) {

        CartEntity cart = getOrCreateActiveCartEntity(currentUser);
        List<CartItemEntity> items = cartItemRepository.findByCartId(cart.getId());

        cartItemRepository.deleteAll(items);
        cartItemRepository.flush();

        return toResponse(recalculateTotal(cart));
    }

    /**
     * Gets the current user's active cart entity or creates a new empty cart.
     */
    private CartEntity getOrCreateActiveCartEntity(UserEntity currentUser) {

        return cartRepository.findByUserIdAndStatus(currentUser.getId(), CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(CartEntity.builder()
                        .user(currentUser)
                        .status(CartStatus.ACTIVE)
                        .totalAmount(BigDecimal.ZERO)
                        .build()));
    }

    /**
     * Loads a product by id or fails when it does not exist.
     */
    private ProductEntity findProductById(UUID productId) {

        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /**
     * Loads a cart item by id or fails when it does not exist.
     */
    private CartItemEntity findCartItemById(UUID cartItemId) {

        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    }

    /**
     * Validates that the product is published and not owned by the buyer.
     */
    private void validateProductCanBeAddedToCart(ProductEntity product, UserEntity currentUser) {

        if (product.getStatus() != PdfProductStatus.PUBLISHED) {
            throw new BadRequestException("Only published products can be added to cart");
        }

        if (product.getSeller().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Buyer cannot add own product to cart");
        }
    }

    /**
     * Recalculates and saves cart total using latest product prices.
     */
    private CartEntity recalculateTotal(CartEntity cart) {

        BigDecimal totalAmount = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(item -> item.getProduct().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(totalAmount);

        return cartRepository.save(cart);
    }

    /**
     * Converts a cart entity into a cart response DTO.
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
     * Converts a cart item entity into a response DTO with current product price.
     */
    private CartItemResponseDTO toItemResponse(CartItemEntity item) {

        return CartItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productTitle(item.getProduct().getTitle())
                .fileType(item.getProduct().getFileType())
                .thumbnailKey(item.getProduct().getThumbnailKey())
                .currentPrice(item.getProduct().getPrice())
                .build();
    }
}

