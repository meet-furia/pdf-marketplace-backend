package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.SellerDashboardResponseDTO;
import com.meet.pdf_marketplace.dto.SellerPaymentDetailsRequestDTO;
import com.meet.pdf_marketplace.dto.SellerPaymentDetailsResponseDTO;
import com.meet.pdf_marketplace.entity.OrderItemEntity;
import com.meet.pdf_marketplace.entity.SellerPaymentDetailsEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.OrderStatus;
import com.meet.pdf_marketplace.repository.OrderItemRepository;
import com.meet.pdf_marketplace.repository.SellerPaymentDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerPaymentDetailsRepository sellerPaymentDetailsRepository;

    private final OrderItemRepository orderItemRepository;

    /**
     * Creates or updates payment details for the current seller.
     * The backend stores details only; no payout is triggered.
     */
    @Transactional
    public SellerPaymentDetailsResponseDTO savePaymentDetails(
            UserEntity currentUser,
            SellerPaymentDetailsRequestDTO request
    ) {

        SellerPaymentDetailsEntity details = sellerPaymentDetailsRepository.findBySellerId(currentUser.getId())
                .orElseGet(() -> SellerPaymentDetailsEntity.builder()
                        .seller(currentUser)
                        .build());

        details.setAccountHolderName(request.getAccountHolderName());
        details.setBankName(request.getBankName());
        details.setAccountNumber(request.getAccountNumber());
        details.setIfscCode(request.getIfscCode());
        details.setUpiId(request.getUpiId());

        return toPaymentResponse(sellerPaymentDetailsRepository.save(details));
    }

    /**
     * Gets payment details for the current seller.
     * Account number is masked in the response.
     */
    @Transactional(readOnly = true)
    public SellerPaymentDetailsResponseDTO getPaymentDetails(UserEntity currentUser) {

        return sellerPaymentDetailsRepository.findBySellerId(currentUser.getId())
                .map(this::toPaymentResponse)
                .orElse(null);
    }

    /**
     * Builds a simple sales dashboard for the current seller.
     * Only paid order items are included in totals.
     */
    @Transactional(readOnly = true)
    public SellerDashboardResponseDTO getDashboard(UserEntity currentUser) {

        List<OrderItemEntity> items = orderItemRepository.findByProductSellerId(currentUser.getId())
                .stream()
                .filter(item -> item.getOrder().getStatus() == OrderStatus.PAID)
                .toList();

        long paidOrderCount = items.stream()
                .map(item -> item.getOrder().getId())
                .distinct()
                .count();

        return SellerDashboardResponseDTO.builder()
                .paidOrderCount(paidOrderCount)
                .soldItemCount((long) items.size())
                .totalSalesAmount(sum(items, OrderItemEntity::getPriceAtPurchase))
                .totalSellerEarning(sum(items, OrderItemEntity::getSellerEarning))
                .totalPlatformFee(sum(items, OrderItemEntity::getPlatformFee))
                .build();
    }

    private BigDecimal sum(List<OrderItemEntity> items, Function<OrderItemEntity, BigDecimal> mapper) {

        return items.stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SellerPaymentDetailsResponseDTO toPaymentResponse(SellerPaymentDetailsEntity details) {

        return SellerPaymentDetailsResponseDTO.builder()
                .id(details.getId())
                .sellerId(details.getSeller().getId())
                .accountHolderName(details.getAccountHolderName())
                .bankName(details.getBankName())
                .maskedAccountNumber(maskAccountNumber(details.getAccountNumber()))
                .ifscCode(details.getIfscCode())
                .upiId(details.getUpiId())
                .build();
    }

    private String maskAccountNumber(String accountNumber) {

        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }

        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
