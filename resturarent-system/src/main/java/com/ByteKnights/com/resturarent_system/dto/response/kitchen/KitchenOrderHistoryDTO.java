package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class KitchenOrderHistoryDTO {
    private Long orderId;
    private String orderNumber;
    private String orderDate;
    private String status;
    private String prepTime; // e.g. "18 min", or "-" if not yet finished cooking
    private List<OrderItemDetailsDTO> items;
}
