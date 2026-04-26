package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderItemDetailsDTO {
    private Long id;
    private String itemName;
    private Integer quantity;
    private String status;
    private String assignedChef;
}
