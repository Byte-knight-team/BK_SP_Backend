package com.ByteKnights.com.resturarent_system.dto.request.inventory;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for restocking an existing inventory item.
 * This is used when new supplies are received at the branch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockInventoryItemRequest {
    /** The amount of stock being added to the current inventory. */
    private BigDecimal quantity;

    /** The cost per unit for this specific batch of items. */
    private BigDecimal unitPrice;

    /** Optional notes describing the restock (e.g., supplier name, batch number). */
    private String notes;
}
