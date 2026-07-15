package com.ByteKnights.com.resturarent_system.dto.request.procurement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePurchaseOrderRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    /** Expected date of delivery — optional but recommended */
    private LocalDate expectedDeliveryDate;

    /** Optional notes for internal record (e.g., "Call vendor to confirm delivery slot") */
    private String notes;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<POLineItemRequest> items;
}
