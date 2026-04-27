package com.ByteKnights.com.resturarent_system.dto.request.inventory;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for correcting or updating an existing inventory item's details.
 * Allows managers to fix errors made during the initial "Add Item" process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventoryItemRequest {
    /** The updated name of the item. */
    private String name;

    /** The category the item belongs to. */
    private String category;

    /** The unit of measurement (e.g., kg, Liters, Pcs). */
    private String unit;

    /** The corrected current stock level. */
    private BigDecimal quantity;

    /** The corrected unit price. */
    private BigDecimal unitPrice;

    /** The updated stock level that triggers a reorder alert. */
    private BigDecimal reorderLevel;

    /** Optional notes describing why the correction was made. */
    private String notes;
}
