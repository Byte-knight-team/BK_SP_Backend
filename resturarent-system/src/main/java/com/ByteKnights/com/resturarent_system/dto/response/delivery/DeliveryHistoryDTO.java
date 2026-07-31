package com.ByteKnights.com.resturarent_system.dto.response.delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryHistoryDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private BigDecimal amount;
    private String status;
    private LocalDateTime completedAt; // Maps to deliveredAt or assignedAt based on status
    private String cancelledReason;
}
