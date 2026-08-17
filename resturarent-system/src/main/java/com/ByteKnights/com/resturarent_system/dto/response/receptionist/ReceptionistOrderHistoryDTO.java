package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReceptionistOrderHistoryDTO {
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String customerPhone;
    private String orderType;
    private String orderDate;
    private String status;
    private String paymentStatus;
    private double finalAmount;
    private List<ReceptionistOrderItemDTO> items;
}
