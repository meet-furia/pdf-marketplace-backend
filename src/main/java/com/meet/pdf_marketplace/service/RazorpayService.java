package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.config.RazorpayProperties;
import com.meet.pdf_marketplace.dto.CreatePaymentRequestDTO;
import com.meet.pdf_marketplace.dto.CreatePaymentResponseDTO;
import com.meet.pdf_marketplace.dto.PaymentInvoiceResponseDTO;
import com.meet.pdf_marketplace.dto.VerifyPaymentRequestDTO;
import com.meet.pdf_marketplace.entity.OrderEntity;
import com.meet.pdf_marketplace.entity.OrderItemEntity;
import com.meet.pdf_marketplace.entity.PaymentInvoiceEntity;
import com.meet.pdf_marketplace.entity.PurchasedPdfEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.enums.OrderStatus;
import com.meet.pdf_marketplace.enums.PaymentStatus;
import com.meet.pdf_marketplace.exception.ResourceNotFoundException;
import com.meet.pdf_marketplace.repository.OrderItemRepository;
import com.meet.pdf_marketplace.repository.OrderRepository;
import com.meet.pdf_marketplace.repository.PaymentInvoiceRepository;
import com.meet.pdf_marketplace.repository.PurchasedPdfRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final RazorpayProperties razorpayProperties;

    private final PaymentInvoiceRepository paymentInvoiceRepository;

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final PurchasedPdfRepository purchasedPdfRepository;

    /**
     * Creates a Razorpay order for an existing pending order.
     * Saves and returns the local payment invoice details.
     */
    @Transactional
    public CreatePaymentResponseDTO createPayment(UserEntity currentUser, CreatePaymentRequestDTO request) {

        OrderEntity order = findOrder(request.getOrderId());
        validateOrderOwner(currentUser, order);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Payment can be created only for pending orders");
        }

        return paymentInvoiceRepository.findByOrderId(order.getId())
                .map(this::toCreateResponse)
                .orElseGet(() -> createRazorpayInvoice(order));
    }

    /**
     * Verifies Razorpay signature and marks payment/order as paid.
     * Purchased PDF records are created only after verification succeeds.
     */
    @Transactional
    public PaymentInvoiceResponseDTO verifyPayment(UserEntity currentUser, VerifyPaymentRequestDTO request) {

        PaymentInvoiceEntity invoice = paymentInvoiceRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment invoice not found"));

        validateOrderOwner(currentUser, invoice.getOrder());

        if (invoice.getStatus() == PaymentStatus.PAID) {
            return toResponse(invoice);
        }

        if (!isValidSignature(request)) {
            invoice.setStatus(PaymentStatus.FAILED);
            paymentInvoiceRepository.save(invoice);
            throw new IllegalArgumentException("Invalid Razorpay payment signature");
        }

        OrderEntity order = invoice.getOrder();

        invoice.setRazorpayPaymentId(request.getRazorpayPaymentId());
        invoice.setRazorpaySignature(request.getRazorpaySignature());
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setStatus(PaymentStatus.PAID);

        order.setStatus(OrderStatus.PAID);

        createPurchasedPdfs(order);
        orderRepository.save(order);

        return toResponse(paymentInvoiceRepository.save(invoice));
    }

    /**
     * Loads an order or fails when the order does not exist.
     */
    private OrderEntity findOrder(UUID orderId) {

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    /**
     * Ensures the authenticated user owns the order being paid.
     */
    private void validateOrderOwner(UserEntity currentUser, OrderEntity order) {

        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Current user does not own this order");
        }
    }

    /**
     * Calls Razorpay and creates the local invoice for the order.
     */
    private CreatePaymentResponseDTO createRazorpayInvoice(OrderEntity order) {

        String razorpayOrderId = createRazorpayOrder(order);

        PaymentInvoiceEntity invoice = PaymentInvoiceEntity.builder()
                .order(order)
                .razorpayOrderId(razorpayOrderId)
                .amount(order.getTotalAmount())
                .currency(resolveCurrency(order))
                .status(PaymentStatus.CREATED)
                .build();

        return toCreateResponse(paymentInvoiceRepository.save(invoice));
    }

    /**
     * Creates a Razorpay order through the Razorpay Java SDK.
     */
    private String createRazorpayOrder(OrderEntity order) {

        ensureRazorpayCredentials();

        try {
            JSONObject options = new JSONObject();
            options.put("amount", toSmallestCurrencyUnit(order.getTotalAmount()));
            options.put("currency", resolveCurrency(order));
            options.put("receipt", order.getId().toString());

            Order razorpayOrder = razorpayClient().orders.create(options);

            return razorpayOrder.get("id");
        } catch (RazorpayException exception) {
            throw new IllegalArgumentException("Razorpay order creation failed: " + exception.getMessage());
        }
    }

    /**
     * Verifies the Razorpay checkout signature using the key secret.
     */
    private boolean isValidSignature(VerifyPaymentRequestDTO request) {

        ensureRazorpayCredentials();

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

    /**
     * Creates purchased PDF records for each order item.
     */
    private void createPurchasedPdfs(OrderEntity order) {

        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(order.getId());

        for (OrderItemEntity item : orderItems) {
            boolean exists = purchasedPdfRepository.existsByUserIdAndProductId(
                    order.getUser().getId(),
                    item.getProduct().getId()
            );

            if (!exists) {
                purchasedPdfRepository.save(PurchasedPdfEntity.builder()
                        .user(order.getUser())
                        .product(item.getProduct())
                        .order(order)
                        .accessGrantedAt(LocalDateTime.now(ZoneOffset.UTC))
                        .build());
            }
        }
    }

    /**
     * Converts an invoice into the create payment response DTO.
     */
    private CreatePaymentResponseDTO toCreateResponse(PaymentInvoiceEntity invoice) {

        return CreatePaymentResponseDTO.builder()
                .invoiceId(invoice.getId())
                .orderId(invoice.getOrder().getId())
                .keyId(razorpayProperties.getKeyId())
                .razorpayOrderId(invoice.getRazorpayOrderId())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .build();
    }

    /**
     * Converts an invoice into the verify payment response DTO.
     */
    private PaymentInvoiceResponseDTO toResponse(PaymentInvoiceEntity invoice) {

        return PaymentInvoiceResponseDTO.builder()
                .id(invoice.getId())
                .orderId(invoice.getOrder().getId())
                .razorpayOrderId(invoice.getRazorpayOrderId())
                .razorpayPaymentId(invoice.getRazorpayPaymentId())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .paymentMethod(invoice.getPaymentMethod())
                .build();
    }

    /**
     * Creates a Razorpay SDK client from configured credentials.
     */
    private RazorpayClient razorpayClient() throws RazorpayException {

        return new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
    }

    /**
     * Converts major currency amount to Razorpay's smallest unit.
     */
    private long toSmallestCurrencyUnit(BigDecimal amount) {

        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /**
     * Resolves the currency used for Razorpay payment creation.
     */
    private String resolveCurrency(OrderEntity order) {

        if (order.getCurrency() != null && !order.getCurrency().isBlank()) {
            return order.getCurrency();
        }

        return razorpayProperties.getCurrency();
    }

    /**
     * Ensures Razorpay credentials are configured before payment operations.
     */
    private void ensureRazorpayCredentials() {

        if (isBlank(razorpayProperties.getKeyId()) || isBlank(razorpayProperties.getKeySecret())) {
            throw new IllegalArgumentException("Razorpay credentials are not configured");
        }
    }

    /**
     * Checks whether a configuration value is blank.
     */
    private boolean isBlank(String value) {

        return value == null || value.isBlank();
    }
}
