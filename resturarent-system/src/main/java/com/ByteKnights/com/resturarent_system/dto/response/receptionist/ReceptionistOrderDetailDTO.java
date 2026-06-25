package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ReceptionistOrderDetailDTO {
    private Long id;
    private String orderNumber;
    private String orderType;
    private String status;
    private String placedAt;

    // Customer
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // Contact (from order form — may differ from account for pickup)
    private String contactName;
    private String contactPhone;
    private String contactEmail;

    // Table (QR only)
    private Integer tableNumber;

    // Items
    private List<ReceptionistOrderItemDTO> items;

    // Payment
    private String paymentStatus;
    private double totalAmount;
    private double taxAmount;
    private double serviceCharge;
    private double discountAmount;
    private String appliedCouponCode;
    private double finalAmount;

    // Notes
    private String kitchenNotes;
    private String holdReason;
    private String cancelReason;
}
