package com.ByteKnights.com.resturarent_system.dto.request.inventory;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
    @NotBlank(message = "Name cannot be empty")
    private String name;

    /** The category the item belongs to. */
    @NotBlank(message = "Category cannot be empty")
    private String category;

    /** The unit of measurement (e.g., kg, Liters, Pcs). */
    @NotBlank(message = "Unit cannot be empty")
    private String unit;

    /** The corrected current stock level. */
    @NotNull(message = "Quantity cannot be null")
    @PositiveOrZero(message = "Quantity cannot be negative")
    private BigDecimal quantity;

    /** The corrected unit price. */
    @NotNull(message = "Unit price cannot be null")
    @PositiveOrZero(message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    /** The updated stock level that triggers a reorder alert. */
    @NotNull(message = "Reorder level cannot be null")
    @PositiveOrZero(message = "Reorder level cannot be negative")
    private BigDecimal reorderLevel;

    /** Optional notes describing why the correction was made. */
    private String notes;
}
