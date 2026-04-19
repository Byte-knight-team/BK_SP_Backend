package com.ByteKnights.com.resturarent_system.dto;

import lombok.*;
import java.util.List;

/**
 * Request payload sent by the frontend when a customer places an online order.
 *
 * Maps from CheckoutPage.jsx fields:
 *   - customerId    → authenticated customer ID
 *   - orderType     → "delivery" | "pickup"  (mapped to OrderType.ONLINE)
 *   - paymentMethodId → selected payment method ID
 *   - tableId       → optional for QR order flows
 *   - items[]       → list of {menuItemId, quantity}
 *   - deliveryAddress → only when orderType == "delivery"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    private Long customerId;
    private String deliveryAddress;     // null for pickup orders
    private String orderType;           // "delivery" or "pickup"
    private Long paymentMethodId;
    private Long tableId;
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
