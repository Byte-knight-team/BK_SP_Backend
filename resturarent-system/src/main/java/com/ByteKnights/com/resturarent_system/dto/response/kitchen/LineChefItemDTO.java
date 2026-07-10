package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LineChefItemDTO {
    private Long itemId;
    private String itemName;
    private Integer quantity;
    private String status;
    private Long orderId;
    private String orderNumber;
    private String orderType;
    private Integer tableNumber;
    private String placedAt;
    private String itemKitchenNotes;
    private String orderKitchenNotes;
}
