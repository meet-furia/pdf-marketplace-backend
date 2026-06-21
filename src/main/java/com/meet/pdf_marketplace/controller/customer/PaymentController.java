package com.meet.pdf_marketplace.controller.customer;

import com.meet.pdf_marketplace.dto.common.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.payment.CreatePaymentRequestDTO;
import com.meet.pdf_marketplace.dto.payment.CreatePaymentResponseDTO;
import com.meet.pdf_marketplace.dto.payment.PaymentInvoiceResponseDTO;
import com.meet.pdf_marketplace.dto.payment.VerifyPaymentRequestDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CheckoutService;
import com.meet.pdf_marketplace.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/customer/payments")
public class PaymentController {

    private final CheckoutService checkoutService;

    private final CurrentUserService currentUserService;

    /**
     * Creates a payment from the current customer's active cart.
     */
    @PostMapping
    public ApiResponseDTO<CreatePaymentResponseDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CreatePaymentResponseDTO payment = checkoutService.createPayment(currentUser, request);

        return ApiResponseDTO.<CreatePaymentResponseDTO>builder()
                .success(true)
                .message("Payment created successfully")
                .data(payment)
                .build();
    }

    /**
     * Verifies a Razorpay payment signature and completes checkout.
     */
    @PostMapping("/verify")
    public ApiResponseDTO<PaymentInvoiceResponseDTO> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        PaymentInvoiceResponseDTO payment = checkoutService.verifyPayment(currentUser, request);

        return ApiResponseDTO.<PaymentInvoiceResponseDTO>builder()
                .success(true)
                .message("Payment verified successfully")
                .data(payment)
                .build();
    }
}

