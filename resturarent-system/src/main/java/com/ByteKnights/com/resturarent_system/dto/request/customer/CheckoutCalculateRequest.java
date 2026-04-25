package com.ByteKnights.com.resturarent_system.dto.request.customer;

import lombok.Data;
import java.util.List;

@Data
public class CheckoutCalculateRequest {
    private String orderType; // ONLINE_PICKUP, ONLINE_DELIVERY, DINE_IN
    private Long branchId;
    
    // Optional Modifiers
    private String couponCode;          
    private Integer redeemLoyaltyPoints; 

    private List<CartItemRequest> items;

    @Data
    public static class CartItemRequest {
        private Long menuItemId;
        private Integer quantity;
    }
}