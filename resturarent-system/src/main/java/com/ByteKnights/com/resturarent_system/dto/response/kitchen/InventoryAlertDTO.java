package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InventoryAlertDTO {
    private String itemName;
    private double percentage;
    private double maxStock; //initial count
    private double availableCount;
    private String unit;
    private String warningLevel; // LOW or CRITICAL
}
