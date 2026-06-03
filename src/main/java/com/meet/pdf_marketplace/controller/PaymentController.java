package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.CreatePaymentRequestDTO;
import com.meet.pdf_marketplace.dto.CreatePaymentResponseDTO;
import com.meet.pdf_marketplace.dto.PaymentInvoiceResponseDTO;
import com.meet.pdf_marketplace.dto.VerifyPaymentRequestDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CurrentUserService;
import com.meet.pdf_marketplace.service.RazorpayService;
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
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final RazorpayService razorpayService;

    private final CurrentUserService currentUserService;

    /**
     * Creates a Razorpay order for a pending local order.
     * Returns the Razorpay order id and local invoice details.
     */
    @PostMapping("/create")
    public ApiResponseDTO<CreatePaymentResponseDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        CreatePaymentResponseDTO payment = razorpayService.createPayment(currentUser, request);

        return ApiResponseDTO.<CreatePaymentResponseDTO>builder()
                .success(true)
                .message("Payment created successfully")
                .data(payment)
                .build();
    }

    /**
     * Verifies a Razorpay payment signature.
     * Marks payment and order as paid only after verification succeeds.
     */
    @PostMapping("/verify")
    public ApiResponseDTO<PaymentInvoiceResponseDTO> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        PaymentInvoiceResponseDTO payment = razorpayService.verifyPayment(currentUser, request);

        return ApiResponseDTO.<PaymentInvoiceResponseDTO>builder()
                .success(true)
                .message("Payment verified successfully")
                .data(payment)
                .build();
    }
}
