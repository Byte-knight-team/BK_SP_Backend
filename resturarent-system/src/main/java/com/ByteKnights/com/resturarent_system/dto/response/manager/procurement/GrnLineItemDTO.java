package com.ByteKnights.com.resturarent_system.dto.response.manager.procurement;

import com.ByteKnights.com.resturarent_system.entity.GrnItemCondition;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GrnLineItemDTO {
    private Long id;
    private Long purchaseOrderItemId;
    private String itemName;
    private BigDecimal orderedQuantity;
    private BigDecimal receivedQuantity;
    private String unit;
    private GrnItemCondition condition;
    private String discrepancyNote;
    /** True if received quantity differs from ordered quantity */
    private boolean hasDiscrepancy;
}
