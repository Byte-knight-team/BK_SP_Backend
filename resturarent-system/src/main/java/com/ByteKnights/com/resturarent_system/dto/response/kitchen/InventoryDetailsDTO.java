package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
//same DTO use to display inventory alerts and display inventory table
public class InventoryDetailsDTO {
    private String name;
    private double percentage;
    private double maxStock; //initial count
    private double quantity; //available count
    private String unit;
    private String warningLevel; // LOW or CRITICAL
}
