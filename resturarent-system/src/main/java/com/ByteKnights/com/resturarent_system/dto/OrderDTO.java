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
    private String customerName;
    private String tableNumber;
    private Integer guestCount;
    private String status;
    private BigDecimal total;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDTO> items;
@Data
@Builder
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private OrderType orderType;
    private BigDecimal totalAmount;
    private String cancelReason;
    private LocalDateTime createdAt;

    // Flattened / Simplified relationships
    private String branchName;
    private String customerName;
    private String tableNumber;

    private List<OrderItemDTO> items;

    @Data
    @Builder
    public static class OrderItemDTO {
        private Long id;
        private String menuItemName;
        private String categoryName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
