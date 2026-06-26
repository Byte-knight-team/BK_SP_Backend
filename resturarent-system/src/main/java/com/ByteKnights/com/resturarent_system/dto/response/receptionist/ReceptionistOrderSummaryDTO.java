package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceptionistOrderSummaryDTO {
    private Long id;
    private String orderNumber;
    private String orderType;       // QR / ONLINE_PICKUP
    private String status;
    private String paymentStatus;   // PAID / PENDING
    private String placedAt;        // formatted time
    private String customerName;
    private String customerPhone;
    private Integer tableNumber;    // null for pickup
    private int totalItems;
    private double finalAmount;
}
