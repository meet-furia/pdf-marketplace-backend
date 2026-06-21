package com.meet.pdf_marketplace.service;

import com.meet.pdf_marketplace.dto.seller.SellerPaymentDetailsRequestDTO;
import com.meet.pdf_marketplace.entity.SellerPaymentDetailsEntity;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.exception.BadRequestException;
import com.meet.pdf_marketplace.repository.OrderItemRepository;
import com.meet.pdf_marketplace.repository.SellerPaymentDetailsRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SellerPaymentDetailsValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidAccountNumberIfscAndUpiFormats() {

        SellerPaymentDetailsRequestDTO request = SellerPaymentDetailsRequestDTO.builder()
                .accountHolderName("Meet Furia")
                .accountNumber("1234ABCD")
                .ifscCode("INVALID")
                .upiId("not-a-upi-id")
                .build();

        assertEquals(3, validator.validate(request).size());
    }

    @Test
    void acceptsValidBankAndUpiFormats() {

        SellerPaymentDetailsRequestDTO request = SellerPaymentDetailsRequestDTO.builder()
                .accountHolderName("Meet Furia")
                .bankName("Example Bank")
                .accountNumber("123456789012")
                .ifscCode("ABCD0123456")
                .upiId("meet.furia@example")
                .build();

        assertEquals(0, validator.validate(request).size());
    }

    @Test
    void preservesSavedAccountNumberWhenUpdateOmitsIt() {

        SellerPaymentDetailsRepository paymentRepository = mock(SellerPaymentDetailsRepository.class);
        SellerService sellerService = new SellerService(paymentRepository, mock(OrderItemRepository.class));
        UserEntity seller = UserEntity.builder().build();
        seller.setId(UUID.randomUUID());
        SellerPaymentDetailsEntity savedDetails = SellerPaymentDetailsEntity.builder()
                .seller(seller)
                .accountHolderName("Meet Furia")
                .bankName("Example Bank")
                .accountNumber("123456789012")
                .ifscCode("ABCD0123456")
                .build();

        when(paymentRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(savedDetails));
        when(paymentRepository.save(any(SellerPaymentDetailsEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        sellerService.savePaymentDetails(
                seller,
                SellerPaymentDetailsRequestDTO.builder()
                        .accountHolderName("Meet Furia")
                        .bankName("Example Bank")
                        .ifscCode("WXYZ0123456")
                        .build()
        );

        assertEquals("123456789012", savedDetails.getAccountNumber());
        assertEquals("WXYZ0123456", savedDetails.getIfscCode());
    }

    @Test
    void rejectsAnIncompleteBankPayoutMethod() {

        SellerPaymentDetailsRepository paymentRepository = mock(SellerPaymentDetailsRepository.class);
        SellerService sellerService = new SellerService(paymentRepository, mock(OrderItemRepository.class));
        UserEntity seller = UserEntity.builder().build();
        seller.setId(UUID.randomUUID());

        when(paymentRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        assertThrows(
                BadRequestException.class,
                () -> sellerService.savePaymentDetails(
                        seller,
                        SellerPaymentDetailsRequestDTO.builder()
                                .accountHolderName("Meet Furia")
                                .bankName("Example Bank")
                                .build()
                )
        );
        verify(paymentRepository, never()).save(any());
    }
}
