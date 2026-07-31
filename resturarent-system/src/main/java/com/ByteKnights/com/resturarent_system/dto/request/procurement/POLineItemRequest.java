package com.ByteKnights.com.resturarent_system.dto.request.procurement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Represents one line item in a CreatePurchaseOrderRequest.
 * inventoryItemId is nullable — null means the item is not yet in the catalog.
 */
@Data
public class POLineItemRequest {

    /**
     * ID of the existing InventoryItem. Null if this is a new item
     * not yet registered in the inventory catalog.
     */
    private Long inventoryItemId;

    /** Plain-text item name — always required regardless of catalog status */
    @NotNull(message = "Item name is required")
    private String itemName;

    @NotNull(message = "Ordered quantity is required")
    @Positive(message = "Ordered quantity must be greater than zero")
    private BigDecimal orderedQuantity;

    @NotNull(message = "Unit is required")
    private String unit;

    /** Agreed price per unit with the vendor. Optional at PO creation stage. */
    private BigDecimal agreedUnitPrice;
}
