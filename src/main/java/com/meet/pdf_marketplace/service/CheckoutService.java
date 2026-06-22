package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.payment.CreatePaymentResponseDTO;
import com.meet.pdf_marketplace.dto.payment.PaymentInvoiceResponseDTO;
import com.meet.pdf_marketplace.dto.payment.VerifyPaymentRequestDTO;
import com.meet.pdf_marketplace.entity.CartEntity;
import com.meet.pdf_marketplace.entity.InvoiceEntity;
import com.meet.pdf_marketplace.entity.OrderEntity;
import com.meet.pdf_marketplace.entity.PaymentEntity;
import com.meet.pdf_marketplace.entity.RazorpayPaymentEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.PaymentStatus;
import com.meet.pdf_marketplace.enums.PaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartService cartService;

    private final PaymentService paymentService;

    private final RazorpayService razorpayService;

    private final RazorpayPaymentService razorpayPaymentService;

    private final OrderService orderService;

    private final InvoiceService invoiceService;

    private final LibraryAccessService libraryAccessService;

    /**
     * Creates or reuses a pending order and prepares its Razorpay payment.
     */
    @Transactional
    public CreatePaymentResponseDTO createOrder(UserEntity currentUser) {

        // Moving the cart to PAYMENT_PENDING prevents edits after prices are snapshotted.
        CartEntity cart = cartService.getOrCreatePaymentPendingCart(currentUser);
        String currency = razorpayService.getCurrency();
        OrderEntity order = orderService.createPaymentPendingOrder(currentUser, cart, currency);
        BigDecimal amount = order.getTotalAmount();
        PaymentEntity payment = paymentService.findCreatedPayment(order);
        RazorpayPaymentEntity razorpayPayment;

        if (payment == null) {
            // Create generic payment state before attaching provider-specific details.
            payment = paymentService.createPayment(
                    order,
                    amount,
                    currency,
                    PaymentProvider.RAZORPAY
            );
            String razorpayOrderId = razorpayService.createOrder(
                    amount,
                    currency,
                    payment.getId().toString()
            );
            razorpayPayment = razorpayPaymentService.create(payment, razorpayOrderId);
        } else {
            razorpayPayment = razorpayPaymentService.getByPayment(payment);
        }

        return toCreateResponse(payment, razorpayPayment);
    }

    /**
     * Verifies Razorpay payment details and completes the order, invoice, and library access.
     */
    @Transactional
    public PaymentInvoiceResponseDTO verifyPayment(UserEntity currentUser, VerifyPaymentRequestDTO request) {

        RazorpayPaymentEntity razorpayPayment =
                razorpayPaymentService.getByRazorpayOrderId(request.getRazorpayOrderId());
        PaymentEntity payment = razorpayPayment.getPayment();
        paymentService.validateOwner(currentUser, payment);

        if (payment.getStatus() == PaymentStatus.PAID) {
            // Razorpay callbacks can be retried; return the existing result idempotently.
            return toVerificationResponse(payment, razorpayPayment, null);
        }

        if (!razorpayService.isValidSignature(request)) {
            // Keep the order pending; the next checkout creates a new payment attempt.
            paymentService.markFailed(payment);
            throw new IllegalArgumentException("Invalid Razorpay payment signature");
        }

        // All post-payment records are created only after signature verification succeeds.
        razorpayPaymentService.markPaid(
                razorpayPayment,
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
        PaymentEntity paidPayment = paymentService.markPaid(payment);
        OrderEntity order = orderService.completePayment(paidPayment.getOrder());
        InvoiceEntity invoice = invoiceService.createForOrder(order, paidPayment);
        libraryAccessService.grantAccessForOrder(order);
        cartService.checkoutCart(order.getCart());

        return toVerificationResponse(paidPayment, razorpayPayment, invoice);
    }

    /**
     * Builds the response used to launch Razorpay checkout.
     */
    private CreatePaymentResponseDTO toCreateResponse(
            PaymentEntity payment,
            RazorpayPaymentEntity razorpayPayment
    ) {

        return CreatePaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .keyId(razorpayService.getKeyId())
                .razorpayOrderId(razorpayPayment.getRazorpayOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .build();
    }

    /**
     * Builds a completed checkout response from generic and Razorpay-specific records.
     */
    private PaymentInvoiceResponseDTO toVerificationResponse(
            PaymentEntity payment,
            RazorpayPaymentEntity razorpayPayment,
            InvoiceEntity invoice
    ) {

        return PaymentInvoiceResponseDTO.builder()
                .id(payment.getId())
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .invoiceId(invoice == null ? null : invoice.getId())
                .invoiceNumber(invoice == null ? null : invoice.getInvoiceNumber())
                .razorpayOrderId(razorpayPayment.getRazorpayOrderId())
                .razorpayPaymentId(razorpayPayment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentProvider(payment.getPaymentProvider())
                .build();
    }
}

