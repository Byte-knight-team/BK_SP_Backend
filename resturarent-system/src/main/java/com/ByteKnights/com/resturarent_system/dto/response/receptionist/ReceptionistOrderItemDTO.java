package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceptionistOrderItemDTO {
    private Long id;
    private String itemName;
    private int quantity;
    private double unitPrice;
    private double subtotal;
    private String status;          // PENDING / PREPARING / READY / SERVED
    private String kitchenNotes;
}
