package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceptionistOrderSummaryDTO {
    private Long id;
    private String orderNumber;
    private String orderType;
    private String status;
    private String paymentStatus;
    private String placedAt;         // createdAt — when the order was first placed
    private String statusUpdatedAt;  // when the status last changed (sent/held/completed/served)
    private String customerName;
    private String customerPhone;
    private Integer tableNumber;
    private int totalItems;
    private double finalAmount;
}
