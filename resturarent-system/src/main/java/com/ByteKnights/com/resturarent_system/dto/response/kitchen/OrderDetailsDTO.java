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
    private LocalDateTime createdAt;
    private LocalDateTime statusUpdatedAt;
    private String status;
    private String holdReason;
    private String kitchenNotes; //kitchen note for the whole order
    private List<OrderItemDetailsDTO> items;
}
