package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LineChefHistoryItemDTO {
    private Long itemId;
    private String itemName;
    private Integer quantity;
    private String status;
    private String orderNumber;
    private Integer tableNumber;
    private String cookedDate;
    private String startTime;
    private String endTime;
}
