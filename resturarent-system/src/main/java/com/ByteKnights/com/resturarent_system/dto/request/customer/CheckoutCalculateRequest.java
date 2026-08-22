package com.ByteKnights.com.resturarent_system.dto.request.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutCalculateRequest {
    @NotNull(message = "Order type is required")
    private String orderType; // ONLINE_PICKUP, ONLINE_DELIVERY, DINE_IN
    
    @NotNull(message = "Branch ID is required")
    private Long branchId;
    
    // Optional Modifiers
    private String couponCode;          
    private Integer redeemLoyaltyPoints; 

    // Delivery Location
    private Double latitude;
    private Double longitude;

    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<CartItemRequest> items;

    @Data
    public static class CartItemRequest {
        @NotNull(message = "Menu item ID is required")
        private Long menuItemId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 50, message = "Quantity cannot exceed 50 per item")
        private Integer quantity;
    }
}