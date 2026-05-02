package com.ByteKnights.com.resturarent_system.dto.response.inventory;

import lombok.*;
import java.math.BigDecimal;

/**
 * Data Transfer Object sent to the frontend to represent an individual
 * inventory item.
 *
 * Maps directly to the shape expected by the frontend's CurrentStockTable.jsx
 * component:
 * - id → id (matches InventoryItem.id)
 * - name → name (matches InventoryItem.name)
 * - category → category (matches InventoryItem.category)
 * - unitPrice → unitPrice (matches InventoryItem.unitPrice)
 * - unit → unit (matches InventoryItem.unit)
 * - stockLevel → stockLevel (mapped from InventoryItem.quantity)
 * - status → "good" | "warning" (derived field: "warning" if quantity <=
 * reorderLevel)
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemDTO {
    private Long id;
    private String name;
    private String category;
    private BigDecimal unitPrice;
    private String unit;
    private BigDecimal stockLevel;
    private String status;
}