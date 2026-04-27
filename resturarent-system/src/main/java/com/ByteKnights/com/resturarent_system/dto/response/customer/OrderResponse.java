package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long orderId;
    private String orderNumber;
    private String orderStatus;
    private String paymentStatus;
    
    // --- Detailed Receipt Breakdown ---
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal deliveryFee;
    private BigDecimal serviceCharge;
    private BigDecimal totalDiscountAmount;
    private BigDecimal finalTotal;
    
    // --- Coupon & Loyalty Details ---
    private String appliedCouponCode;
    private Integer rewardPointsRedeemed;
    private Integer rewardPointsEarned;
    
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    @Data
    @Builder
    public static class OrderItemResponse {
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}