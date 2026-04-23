package com.ByteKnights.com.resturarent_system.dto.request.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Data Transfer Object representing a request to create a new inventory item.
 *
 * This DTO is used by the frontend to send the details of a new item
 * to the backend. It includes validation constraints to ensure data
 * integrity before processing.
 *
 * Fields:
 * - name: The unique name of the inventory item.
 * - category: The classification category (e.g., Grains, Dairy).
 * - quantity: The initial stock level to be recorded.
 * - unit: The unit of measurement (e.g., kg, Liter, Pcs).
 * - reorderLevel: The threshold at which a low stock warning is triggered.
 * - unitPrice: The cost per unit of the item.
 * - branchId: Optional ID used by SUPER_ADMINs to specify a target branch.
 */
@Data
public class CreateInventoryItemRequest {

    /**
     * The name of the inventory item.
     * Must not be blank.
     */
    @NotBlank(message = "Item name is required")
    private String name;

    /**
     * The category classification of the item.
     * Must not be blank.
     */
    @NotBlank(message = "Category is required")
    private String category;

    /**
     * The initial quantity of the item in stock.
     * Must not be null and must be zero or positive.
     */
    @NotNull(message = "Initial quantity is required")
    @PositiveOrZero(message = "Quantity cannot be negative")
    private BigDecimal quantity;

    /**
     * The unit of measurement for the item's quantity.
     * Must not be blank.
     */
    @NotBlank(message = "Unit is required")
    private String unit;

    /**
     * The stock level threshold for reordering.
     * Must not be null and must be zero or positive.
     */
    @NotNull(message = "Reorder level is required")
    @PositiveOrZero(message = "Reorder level cannot be negative")
    private BigDecimal reorderLevel;

    /**
     * The price per unit of the item.
     * Must not be null and must be zero or positive.
     */
    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    /**
     * Optional branch ID to assign the item to.
     * Only utilized when the authenticated user has the SUPER_ADMIN role.
     */
    private Long branchId;
}
