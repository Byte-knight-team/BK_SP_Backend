package com.ByteKnights.com.resturarent_system.dto.request.procurement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateGrnRequest {

    @NotNull(message = "Purchase Order ID is required")
    private Long purchaseOrderId;

    /** Optional vendor invoice/delivery note reference for accounting */
    private String invoiceReference;

    /** Optional notes about the delivery */
    private String notes;

    @NotEmpty(message = "At least one received item is required")
    @Valid
    private List<GrnLineItemRequest> items;
}
