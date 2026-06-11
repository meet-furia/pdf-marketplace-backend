package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.payment.CreatePaymentResponseDTO;
import com.meet.pdf_marketplace.dto.payment.PaymentInvoiceResponseDTO;
import com.meet.pdf_marketplace.dto.payment.VerifyPaymentRequestDTO;
import com.meet.pdf_marketplace.entity.CartEntity;
import com.meet.pdf_marketplace.entity.InvoiceEntity;
import com.meet.pdf_marketplace.entity.OrderEntity;
import com.meet.pdf_marketplace.entity.PaymentEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.PaymentStatus;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String DEFAULT_PAYMENT_METHOD = "RAZORPAY";

    private final PaymentRepository paymentRepository;

    private final RazorpayGateway razorpayGateway;

    public PaymentEntity createPayment(UserEntity currentUser, CartEntity cart, BigDecimal amount, String currency) {

        return paymentRepository.findByCartIdAndStatus(cart.getId(), PaymentStatus.CREATED)
                .orElseGet(() -> paymentRepository.save(PaymentEntity.builder()
                        .user(currentUser)
                        .cart(cart)
                        .razorpayOrderId(razorpayGateway.createOrder(amount, currency, cart.getId().toString()))
                        .amount(amount)
                        .currency(currency)
                        .status(PaymentStatus.CREATED)
                        .build()));
    }

    public PaymentEntity getByRazorpayOrderId(String razorpayOrderId) {

        return paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    public void validateOwner(UserEntity currentUser, PaymentEntity payment) {

        if (!payment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Current user does not own this payment");
        }
    }

    public boolean isSignatureValid(VerifyPaymentRequestDTO request) {

        return razorpayGateway.isValidSignature(request);
    }

    public PaymentEntity markPaid(PaymentEntity payment, VerifyPaymentRequestDTO request) {

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setPaymentMethod(resolvePaymentMethod(request));
        payment.setStatus(PaymentStatus.PAID);

        return paymentRepository.save(payment);
    }

    public PaymentEntity markFailed(PaymentEntity payment) {

        payment.setStatus(PaymentStatus.FAILED);

        return paymentRepository.save(payment);
    }

    public CreatePaymentResponseDTO toCreateResponse(PaymentEntity payment) {

        return CreatePaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder() == null ? null : payment.getOrder().getId())
                .keyId(razorpayGateway.getKeyId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .build();
    }

    public PaymentInvoiceResponseDTO toVerificationResponse(PaymentEntity payment) {

        return toVerificationResponse(payment, null);
    }

    public PaymentInvoiceResponseDTO toVerificationResponse(PaymentEntity payment, InvoiceEntity invoice) {

        OrderEntity order = payment.getOrder();

        return PaymentInvoiceResponseDTO.builder()
                .id(payment.getId())
                .paymentId(payment.getId())
                .orderId(order == null ? null : order.getId())
                .invoiceId(invoice == null ? null : invoice.getId())
                .invoiceNumber(invoice == null ? null : invoice.getInvoiceNumber())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .build();
    }

    public String resolveCurrency() {

        return razorpayGateway.getCurrency();
    }

    private String resolvePaymentMethod(VerifyPaymentRequestDTO request) {

        return request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()
                ? DEFAULT_PAYMENT_METHOD
                : request.getPaymentMethod();
    }
}

