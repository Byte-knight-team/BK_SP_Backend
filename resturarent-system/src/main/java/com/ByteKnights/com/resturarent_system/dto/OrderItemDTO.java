package com.ByteKnights.com.resturarent_system.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {
    private Long id;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String kitchenNotes;
}
