package com.ByteKnights.com.resturarent_system.dto.request.customer;

import com.ByteKnights.com.resturarent_system.entity.PaymentMethod;
import lombok.Data;
import java.util.List;

@Data
public class PlaceOrderRequest {
    private String orderType; 
    private Long branchId;
    private Long tableId; // Only required for DINE_IN
    
    // Identical cart data for the Zero-Trust calculation
    private String couponCode;
    private Integer redeemLoyaltyPoints;
    private List<CheckoutCalculateRequest.CartItemRequest> items;

    // Final Checkout Details
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String deliveryAddress;
    private String kitchenNotes;
    private PaymentMethod paymentMethod; // CASH or CARD
}