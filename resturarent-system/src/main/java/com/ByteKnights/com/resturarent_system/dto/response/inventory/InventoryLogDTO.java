package com.ByteKnights.com.resturarent_system.dto.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single entry in the Inventory Update Log.
 * This is sent to the frontend to populate the "Inventory Update Log" table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLogDTO {
    /** The name of the inventory item that was updated. */
    private String itemName;

    /** The formatted date and time when the update occurred (e.g., "2024-04-27 08:30 AM"). */
    private String updatedAt;

    /** The type of update (e.g., "RESTOCK", "WASTAGE", "CORRECTION"). */
    private String updateType;

    /** The notes or reasons provided by the manager for this update. */
    private String notes;
}
