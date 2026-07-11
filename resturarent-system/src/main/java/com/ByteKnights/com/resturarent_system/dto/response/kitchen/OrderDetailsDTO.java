package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderDetailsDTO {
    private Long id;
    private String orderNumber;
    private LocalDateTime createdAt;
    private LocalDateTime statusUpdatedAt;
    private String status;
    private String holdReason;
    private String kitchenNotes;
    private List<OrderItemDetailsDTO> items;
}
