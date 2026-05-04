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
    private String orderType;
    private String paymentStatus;
    private String paymentMethod;
    private String cancellationReason;
    private Boolean isReviewed;
    
    // --- Contact & Delivery ---
    private String contactName;
    private String contactPhone;
    private String deliveryAddress;
    private String kitchenNotes;
    private Long tableId;
    private Integer tableNumber;
    
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
    private BranchDetailResponse branchDetails;
    private List<OrderItemResponse> items;

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long orderItemId;
        private Long menuItemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private Boolean isReviewed;
    }
}