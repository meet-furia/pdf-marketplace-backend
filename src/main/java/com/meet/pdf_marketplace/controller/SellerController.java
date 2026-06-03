package com.meet.pdf_marketplace.controller;

import com.meet.pdf_marketplace.dto.ApiResponseDTO;
import com.meet.pdf_marketplace.dto.SellerDashboardResponseDTO;
import com.meet.pdf_marketplace.dto.SellerPaymentDetailsRequestDTO;
import com.meet.pdf_marketplace.dto.SellerPaymentDetailsResponseDTO;
import com.meet.pdf_marketplace.entity.UserEntity;
import com.meet.pdf_marketplace.service.CurrentUserService;
import com.meet.pdf_marketplace.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/seller")
public class SellerController {

    private final SellerService sellerService;

    private final CurrentUserService currentUserService;

    /**
     * Saves seller payment details for future payouts.
     * The backend stores details only; no payout is triggered.
     */
    @PostMapping("/payment-details")
    public ApiResponseDTO<SellerPaymentDetailsResponseDTO> savePaymentDetails(
            @Valid @RequestBody SellerPaymentDetailsRequestDTO request
    ) {

        UserEntity currentUser = currentUserService.getCurrentUser();
        SellerPaymentDetailsResponseDTO details = sellerService.savePaymentDetails(currentUser, request);

        return ApiResponseDTO.<SellerPaymentDetailsResponseDTO>builder()
                .success(true)
                .message("Seller payment details saved successfully")
                .data(details)
                .build();
    }

    /**
     * Gets seller payment details for the current user.
     * Account number is masked in the response.
     */
    @GetMapping("/payment-details")
    public ApiResponseDTO<SellerPaymentDetailsResponseDTO> getPaymentDetails() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        SellerPaymentDetailsResponseDTO details = sellerService.getPaymentDetails(currentUser);

        return ApiResponseDTO.<SellerPaymentDetailsResponseDTO>builder()
                .success(true)
                .message("Seller payment details fetched successfully")
                .data(details)
                .build();
    }

    /**
     * Gets a simple seller dashboard for paid orders.
     * Totals are calculated from paid order items.
     */
    @GetMapping("/dashboard")
    public ApiResponseDTO<SellerDashboardResponseDTO> getDashboard() {

        UserEntity currentUser = currentUserService.getCurrentUser();
        SellerDashboardResponseDTO dashboard = sellerService.getDashboard(currentUser);

        return ApiResponseDTO.<SellerDashboardResponseDTO>builder()
                .success(true)
                .message("Seller dashboard fetched successfully")
                .data(dashboard)
                .build();
    }
}
