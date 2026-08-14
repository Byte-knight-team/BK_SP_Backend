package com.ByteKnights.com.resturarent_system.dto.request.procurement;

import com.ByteKnights.com.resturarent_system.entity.GrnItemCondition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Represents one received item line in a CreateGrnRequest.
 */
@Data
public class GrnLineItemRequest {

    @NotNull(message = "Purchase order item ID is required")
    private Long purchaseOrderItemId;

    @NotNull(message = "Received quantity is required")
    @PositiveOrZero(message = "Received quantity cannot be negative")
    private BigDecimal receivedQuantity;

    /** Condition of the received goods. Defaults to GOOD if not provided. */
    private GrnItemCondition condition = GrnItemCondition.GOOD;

    /** Optional manual discrepancy note from the manager */
    private String discrepancyNote;
}
