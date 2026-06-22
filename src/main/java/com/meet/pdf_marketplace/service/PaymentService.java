package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.entity.OrderEntity;
import com.meet.pdf_marketplace.entity.PaymentEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.PaymentStatus;
import com.meet.pdf_marketplace.enums.PaymentProvider;
import com.meet.pdf_marketplace.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * Finds the reusable created payment attempt for an order, if one exists.
     */
    public PaymentEntity findCreatedPayment(OrderEntity order) {

        return paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.CREATED)
                .orElse(null);
    }

    /**
     * Persists an internal payment record using references returned by a payment provider.
     */
    public PaymentEntity createPayment(
            OrderEntity order,
            BigDecimal amount,
            String currency,
            PaymentProvider paymentProvider
    ) {

        return paymentRepository.save(PaymentEntity.builder()
                .order(order)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.CREATED)
                .paymentProvider(paymentProvider)
                .build());
    }

    /**
     * Ensures the authenticated customer owns the payment being processed.
     */
    public void validateOwner(UserEntity currentUser, PaymentEntity payment) {

        // Payment ownership is derived through its authoritative order relationship.
        if (!payment.getOrder().getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Current user does not own this payment");
        }
    }

    /**
     * Marks a successfully verified generic payment as paid.
     */
    public PaymentEntity markPaid(PaymentEntity payment) {

        payment.setStatus(PaymentStatus.PAID);

        return paymentRepository.save(payment);
    }

    /**
     * Marks an unsuccessful payment attempt as failed.
     */
    public PaymentEntity markFailed(PaymentEntity payment) {

        payment.setStatus(PaymentStatus.FAILED);

        return paymentRepository.save(payment);
    }
}

