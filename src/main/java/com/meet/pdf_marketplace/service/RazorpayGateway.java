package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.config.RazorpayProperties;
import com.meet.pdf_marketplace.dto.payment.VerifyPaymentRequestDTO;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RazorpayGateway {

    private final RazorpayProperties razorpayProperties;

    public String createOrder(BigDecimal amount, String currency, String receipt) {

        ensureCredentials();

        try {
            JSONObject options = new JSONObject();
            options.put("amount", toSmallestCurrencyUnit(amount));
            options.put("currency", currency);
            options.put("receipt", receipt);

            Order razorpayOrder = razorpayClient().orders.create(options);

            return razorpayOrder.get("id");
        } catch (RazorpayException exception) {
            throw new IllegalArgumentException("Razorpay order creation failed: " + exception.getMessage());
        }
    }

    public boolean isValidSignature(VerifyPaymentRequestDTO request) {

        ensureCredentials();

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            return Utils.verifyPaymentSignature(options, razorpayProperties.getKeySecret());
        } catch (RazorpayException exception) {
            throw new IllegalArgumentException("Unable to verify Razorpay payment signature");
        }
    }

    public String getKeyId() {

        return razorpayProperties.getKeyId();
    }

    public String getCurrency() {

        return isBlank(razorpayProperties.getCurrency()) ? "INR" : razorpayProperties.getCurrency();
    }

    private RazorpayClient razorpayClient() throws RazorpayException {

        return new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
    }

    private long toSmallestCurrencyUnit(BigDecimal amount) {

        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private void ensureCredentials() {

        if (isBlank(razorpayProperties.getKeyId()) || isBlank(razorpayProperties.getKeySecret())) {
            throw new IllegalArgumentException("Razorpay credentials are not configured");
        }
    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();
    }
}

