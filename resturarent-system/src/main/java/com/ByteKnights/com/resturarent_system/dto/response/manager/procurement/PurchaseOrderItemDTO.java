package com.ByteKnights.com.resturarent_system.dto.response.manager.procurement;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PurchaseOrderItemDTO {
    private Long id;
    private Long inventoryItemId;
    private String itemNameSnapshot;
    private BigDecimal orderedQuantity;
    private String unit;
    private BigDecimal agreedUnitPrice;
    /** True if this line item has a linked InventoryItem in the catalog */
    private boolean linkedToCatalog;
    /** Total quantity already received (GOOD condition) across all GRNs for this item */
    private BigDecimal totalReceivedQuantity;
}
