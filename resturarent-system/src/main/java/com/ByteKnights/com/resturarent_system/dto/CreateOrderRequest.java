package com.ByteKnights.com.resturarent_system.dto;

import lombok.*;
import java.util.List;

/**
 * Request payload sent by the frontend when a customer places an online order.
 *
 * Maps from CheckoutPage.jsx fields:
 *   - customerName  → fullName
 *   - customerPhone → phone
 *   - orderType     → "delivery" | "pickup"  (mapped to OrderType.ONLINE)
 *   - paymentMethod → "pay-now" | "pay-later" (mapped to PaymentMethod enum)
 *   - items[]       → list of {menuItemId, quantity}
 *   - deliveryAddress → only when orderType == "delivery"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    private String customerName;
    private String customerPhone;
    private String deliveryAddress;     // null for pickup orders
    private String orderType;           // "delivery" or "pickup"
    private String paymentMethod;       // "pay-now" or "pay-later"
    private Long branchId;              // which branch to order from
    private List<OrderItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {
        private Long menuItemId;
        private Integer quantity;
    }
}
