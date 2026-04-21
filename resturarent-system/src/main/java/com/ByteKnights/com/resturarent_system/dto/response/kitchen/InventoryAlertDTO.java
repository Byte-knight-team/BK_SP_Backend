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
    private double maxStock;   // Frontend එකේ නමටම ගැලපෙන්න මම මේක initialCount කියලා දැම්මා
    private double availableCount;
    private String unit;
    private String warningLevel;   // LOW or CRITICAL
}
