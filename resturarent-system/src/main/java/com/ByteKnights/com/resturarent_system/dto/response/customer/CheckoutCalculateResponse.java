package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CheckoutCalculateResponse {
    // Standard Math
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal deliveryFee;
    private BigDecimal serviceCharge;
    
    // --- Detailed Discount Breakdown ---
    private BigDecimal couponDiscountAmount;
    private BigDecimal loyaltyDiscountAmount;
    private BigDecimal totalDiscountAmount; // The sum of the two above
    
    private BigDecimal finalTotal;
    
    // Interactive UI Details
    private String appliedCouponCode;
    private Integer loyaltyPointsRedeemed; // Renamed to match the JSON exactly
    private Integer availableLoyaltyPoints;
    private Integer loyaltyPointsEarnedThisOrder;
    private Integer minPointsToRedeem;
    private Integer maxRedeemablePoints; // Backend-calculated 50% cap
}