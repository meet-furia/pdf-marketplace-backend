package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.entity.PaymentEntity;
import com.meet.pdf_marketplace.entity.RazorpayPaymentEntity;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.RazorpayPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RazorpayPaymentService {

    private final RazorpayPaymentRepository razorpayPaymentRepository;

    /**
     * Persists Razorpay-specific data for a generic payment attempt.
     */
    public RazorpayPaymentEntity create(PaymentEntity payment, String razorpayOrderId) {

        return razorpayPaymentRepository.save(RazorpayPaymentEntity.builder()
                .payment(payment)
                .razorpayOrderId(razorpayOrderId)
                .build());
    }

    /**
     * Loads Razorpay details belonging to a generic payment.
     */
    public RazorpayPaymentEntity getByPayment(PaymentEntity payment) {

        return razorpayPaymentRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Razorpay payment details not found"));
    }

    /**
     * Loads Razorpay details using Razorpay's external order identifier.
     */
    public RazorpayPaymentEntity getByRazorpayOrderId(String razorpayOrderId) {

        return razorpayPaymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Razorpay payment details not found"));
    }

    /**
     * Stores successful Razorpay payment references after signature verification.
     */
    public RazorpayPaymentEntity markPaid(
            RazorpayPaymentEntity razorpayPayment,
            String razorpayPaymentId,
            String razorpaySignature
    ) {

        // Provider proof is persisted separately from generic payment status.
        razorpayPayment.setRazorpayPaymentId(razorpayPaymentId);
        razorpayPayment.setRazorpaySignature(razorpaySignature);

        return razorpayPaymentRepository.save(razorpayPayment);
    }
}
