package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.payment.CreatePaymentRequestDTO;
import com.meet.pdf_marketplace.dto.payment.CreatePaymentResponseDTO;
import com.meet.pdf_marketplace.dto.payment.PaymentInvoiceResponseDTO;
import com.meet.pdf_marketplace.dto.payment.VerifyPaymentRequestDTO;
import com.meet.pdf_marketplace.entity.CartEntity;
import com.meet.pdf_marketplace.entity.InvoiceEntity;
import com.meet.pdf_marketplace.entity.OrderEntity;
import com.meet.pdf_marketplace.entity.PaymentEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartService cartService;

    private final PaymentService paymentService;

    private final OrderService orderService;

    private final InvoiceService invoiceService;

    private final LibraryAccessService libraryAccessService;

    @Transactional
    public CreatePaymentResponseDTO createPayment(UserEntity currentUser, CreatePaymentRequestDTO request) {

        CartEntity cart = cartService.getOrCreatePaymentPendingCart(currentUser);
        BigDecimal amount = cartService.requireCartTotal(cart);
        PaymentEntity payment = paymentService.createPayment(
                currentUser,
                cart,
                amount,
                paymentService.resolveCurrency()
        );

        return paymentService.toCreateResponse(payment);
    }

    @Transactional
    public PaymentInvoiceResponseDTO verifyPayment(UserEntity currentUser, VerifyPaymentRequestDTO request) {

        PaymentEntity payment = paymentService.getByRazorpayOrderId(request.getRazorpayOrderId());
        paymentService.validateOwner(currentUser, payment);

        if (payment.getStatus() == PaymentStatus.PAID) {
            return paymentService.toVerificationResponse(payment);
        }

        if (!paymentService.isSignatureValid(request)) {
            paymentService.markFailed(payment);
            cartService.releasePaymentPendingCart(payment.getCart());
            throw new IllegalArgumentException("Invalid Razorpay payment signature");
        }

        PaymentEntity paidPayment = paymentService.markPaid(payment, request);
        OrderEntity order = orderService.createFromPaidPayment(paidPayment);
        InvoiceEntity invoice = invoiceService.createForOrder(order, paidPayment);
        libraryAccessService.grantAccessForOrder(order);
        cartService.checkoutCart(paidPayment.getCart());

        return paymentService.toVerificationResponse(paidPayment, invoice);
    }
}

