package com.ByteKnights.com.resturarent_system.dto;

import lombok.*;

import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.OrderType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private Long branchId;
    private Long tableId;
    private String status;
    private OrderStatus orderStatusEnum;
    private OrderType orderType;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String paymentStatus;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String branchName;
    private List<OrderItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Long id;
        private String itemName;
        private String menuItemName;
        private String categoryName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private String kitchenNotes;
    }
}
