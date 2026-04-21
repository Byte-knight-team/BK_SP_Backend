package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class KitchenOrderDTO {
    private Long id;
    private String status;
    private String time;    // ex: "10:30 AM"
    private int itemCount;
}
