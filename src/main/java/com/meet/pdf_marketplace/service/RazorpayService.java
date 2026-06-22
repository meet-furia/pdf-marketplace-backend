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
public class RazorpayService {

    private final RazorpayProperties razorpayProperties;

    /**
     * Creates a Razorpay order and returns its external order identifier.
     */
    public String createOrder(BigDecimal amount, String currency, String receipt) {

        ensureCredentials();

        try {
            // Razorpay expects the amount in the currency's smallest unit.
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

    /**
     * Verifies that the payment callback was signed by Razorpay.
     */
    public boolean isValidSignature(VerifyPaymentRequestDTO request) {

        ensureCredentials();

        try {
            // These key names are fixed by Razorpay's signature verification contract.
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            return Utils.verifyPaymentSignature(options, razorpayProperties.getKeySecret());
        } catch (RazorpayException exception) {
            throw new IllegalArgumentException("Unable to verify Razorpay payment signature");
        }
    }

    /**
     * Returns the public Razorpay key used by the frontend checkout widget.
     */
    public String getKeyId() {

        return razorpayProperties.getKeyId();
    }

    /**
     * Returns the configured Razorpay currency, defaulting to INR.
     */
    public String getCurrency() {

        return isBlank(razorpayProperties.getCurrency()) ? "INR" : razorpayProperties.getCurrency();
    }

    /**
     * Creates an authenticated Razorpay SDK client.
     */
    private RazorpayClient razorpayClient() throws RazorpayException {

        return new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
    }

    /**
     * Converts a decimal amount to the smallest currency unit expected by Razorpay.
     */
    private long toSmallestCurrencyUnit(BigDecimal amount) {

        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /**
     * Fails fast when Razorpay credentials are not configured.
     */
    private void ensureCredentials() {

        if (isBlank(razorpayProperties.getKeyId()) || isBlank(razorpayProperties.getKeySecret())) {
            throw new IllegalArgumentException("Razorpay credentials are not configured");
        }
    }

    /**
     * Checks whether a configuration value is absent or blank.
     */
    private boolean isBlank(String value) {

        return value == null || value.isBlank();
    }
}