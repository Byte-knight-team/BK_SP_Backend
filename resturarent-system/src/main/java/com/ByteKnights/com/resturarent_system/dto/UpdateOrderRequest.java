package com.ByteKnights.com.resturarent_system.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {
    private String tableNumber;
    private List<UpdateOrderItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateOrderItemRequest {
        private Long id;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private String kitchenNotes;
    }
}
