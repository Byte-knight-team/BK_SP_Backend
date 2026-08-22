package com.ByteKnights.com.resturarent_system.dto.request.customer;

import com.ByteKnights.com.resturarent_system.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class PlaceOrderRequest {
    @NotNull(message = "Order type is required")
    private String orderType; 
    
    @NotNull(message = "Branch ID is required")
    private Long branchId;
    
    private Long tableId; // Only required for DINE_IN
    private Long qrSessionId; // Required for QR orders — validates active session
    
    // Identical cart data for the Zero-Trust calculation
    private String couponCode;
    private Integer redeemLoyaltyPoints;
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<PlaceOrderItemRequest> items;

    // Final Checkout Details
    @NotBlank(message = "Contact name is required")
    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    private String contactName;

    @NotBlank(message = "Contact phone number is required")
    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    private String contactPhone;

    @Size(max = 100, message = "Contact email must not exceed 100 characters")
    private String contactEmail;

    @Size(max = 500, message = "Delivery address must not exceed 500 characters")
    private String deliveryAddress;

    private Double latitude;
    private Double longitude;
    
    @Size(max = 500, message = "Kitchen notes must not exceed 500 characters")
    private String kitchenNotes;
    
    private PaymentMethod paymentMethod; // CASH or CARD

    @Data
    public static class PlaceOrderItemRequest {
        @NotNull(message = "Menu item ID is required")
        private Long menuItemId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 50, message = "Quantity cannot exceed 50 per item")
        private Integer quantity;

        @Size(max = 255, message = "Kitchen note must not exceed 255 characters")
        private String kitchenNote;
    }
}