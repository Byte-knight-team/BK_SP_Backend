package com.ByteKnights.com.resturarent_system.dto.request.inventory;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for removing stock from an existing inventory item.
 * Typically used for wastage, spoilage, or damaged items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveInventoryStockRequest {
    /** The amount of stock to be removed from the current inventory. */
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    /** Mandatory reason for the removal (e.g., "Spoiled", "Dropped"). */
    @NotBlank(message = "Reason cannot be empty")
    private String reason;
}
