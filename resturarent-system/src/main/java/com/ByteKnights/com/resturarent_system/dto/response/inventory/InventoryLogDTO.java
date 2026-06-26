package com.ByteKnights.com.resturarent_system.dto.response.inventory;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single entry in the Inventory Update Log.
 * This is sent to the frontend to populate the "Inventory Update Log" table
 * and the Log Detail popup modal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLogDTO {
    /** Unique transaction ID for identifying the log entry. */
    private Long id;

    /** The name of the inventory item that was updated. */
    private String itemName;

    /** The category of the inventory item (e.g., "Meat", "Dairy"). */
    private String category;

    /** The unit of measurement (e.g., "kg", "Liters"). */
    private String unit;

    /** The formatted date and time when the update occurred (e.g., "2024-04-27 08:30 AM"). */
    private String updatedAt;

    /** The type of update (e.g., "RESTOCK", "WASTAGE", "CORRECTION"). */
    private String updateType;

    /** The amount of quantity added or removed during this transaction. */
    private BigDecimal quantityChange;

    /** The stock level BEFORE this transaction. */
    private BigDecimal previousQuantity;

    /** The stock level AFTER this transaction. */
    private BigDecimal newQuantity;

    /** The unit price recorded at the time of the transaction. */
    private BigDecimal unitPrice;

    /** The name of the staff member who performed the update. */
    private String performedBy;

    /** The notes or reasons provided by the manager for this update. */
    private String notes;
}
