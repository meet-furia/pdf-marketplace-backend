package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.entity.InvoiceEntity;
import com.meet.pdf_marketplace.entity.OrderEntity;
import com.meet.pdf_marketplace.entity.PaymentEntity;
import com.meet.pdf_marketplace.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceEntity createForOrder(OrderEntity order, PaymentEntity payment) {

        return invoiceRepository.findByOrderId(order.getId())
                .orElseGet(() -> invoiceRepository.save(InvoiceEntity.builder()
                        .order(order)
                        .payment(payment)
                        .invoiceNumber(buildInvoiceNumber(order))
                        .totalAmount(order.getTotalAmount())
                        .currency(order.getCurrency())
                        .issuedAt(LocalDateTime.now(ZoneOffset.UTC))
                        .build()));
    }

    private String buildInvoiceNumber(OrderEntity order) {

        String date = DateTimeFormatter.BASIC_ISO_DATE.format(LocalDateTime.now(ZoneOffset.UTC));
        String shortOrderId = order.getId().toString().substring(0, 8).toUpperCase();

        return "INV-" + date + "-" + shortOrderId;
    }
}

