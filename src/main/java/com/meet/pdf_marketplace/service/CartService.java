package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.cart.AddCartItemRequestDTO;
import com.meet.pdf_marketplace.dto.cart.CartItemResponseDTO;
import com.meet.pdf_marketplace.dto.cart.CartResponseDTO;
import com.meet.pdf_marketplace.entity.CartElementEntity;
import com.meet.pdf_marketplace.entity.CartEntity;
import com.meet.pdf_marketplace.entity.PdfProductEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.CartStatus;
import com.meet.pdf_marketplace.enums.PdfProductStatus;
import com.meet.pdf_marketplace.exception.BadRequestException;
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

        validateProductIsPublished(product);

        if (product.getSeller().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Buyer cannot add own product to cart");
        }

        if (cartItemRepository.existsByCartIdAndProductId(cart.getId(), product.getId())) {
            throw new IllegalArgumentException("Product already exists in active cart");
        }

        CartElementEntity item = CartElementEntity.builder()
                .cart(cart)
                .product(product)
                .priceAtTime(product.getPrice())
                .build();

        cartItemRepository.save(item);

        return toResponse(recalculateTotal(cart));
    }

    /**
     * Freezes the active cart while payment is pending.
     */
    public CartEntity getOrCreatePaymentPendingCart(UserEntity currentUser) {

        return cartRepository.findByUserIdAndStatus(currentUser.getId(), CartStatus.PAYMENT_PENDING)
                .orElseGet(() -> {
                    CartEntity cart = cartRepository.findByUserIdAndStatus(currentUser.getId(), CartStatus.ACTIVE)
                            .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

                    requireCartTotal(cart);
                    cart.setStatus(CartStatus.PAYMENT_PENDING);

                    return cartRepository.save(cart);
                });
    }

    /**
     * Returns a non-zero cart total or fails when the cart is empty.
     */
    public BigDecimal requireCartTotal(CartEntity cart) {

        CartEntity recalculatedCart = recalculateTotal(cart);

        if (recalculatedCart.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cart must not be empty");
        }

        return recalculatedCart.getTotalAmount();
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
     * Marks a paid cart as checked out.
     */
    public CartEntity checkoutCart(CartEntity cart) {

        cart.setStatus(CartStatus.CHECKED_OUT);

        return cartRepository.save(cart);
    }

    /**
     * Removes one item from a cart.
     * Recalculates the cart total after removal.
     */
    @Transactional
    public CartResponseDTO removeItem(UserEntity currentUser, UUID cartItemId) {

        CartElementEntity item = findCartItem(cartItemId);
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
        List<CartElementEntity> items = cartItemRepository.findByCartId(cart.getId());

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
     * Allows only approved and published products to be added to cart.
     */
    private void validateProductIsPublished(PdfProductEntity product) {

        if (product.getStatus() != PdfProductStatus.PUBLISHED) {
            throw new BadRequestException("Only published products can be added to cart");
        }
    }

    /**
     * Loads a cart item or fails when the cart item does not exist.
     */
    private CartElementEntity findCartItem(UUID cartItemId) {

        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    }

    /**
     * Recalculates and saves the cart total from current item prices.
     */
    private CartEntity recalculateTotal(CartEntity cart) {

        BigDecimal totalAmount = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(CartElementEntity::getPriceAtTime)
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
    private CartItemResponseDTO toItemResponse(CartElementEntity item) {

        return CartItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productTitle(item.getProduct().getTitle())
                .thumbnailKey(item.getProduct().getThumbnailKey())
                .priceAtTime(item.getPriceAtTime())
                .build();
    }
}

