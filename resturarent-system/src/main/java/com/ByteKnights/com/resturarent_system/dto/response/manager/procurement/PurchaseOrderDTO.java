package com.ByteKnights.com.resturarent_system.dto.response.manager.procurement;

import com.ByteKnights.com.resturarent_system.entity.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PurchaseOrderDTO {
    private Long id;
    private String poNumber;
    private Long vendorId;
    private String vendorName;
    private PurchaseOrderStatus status;
    private LocalDate expectedDeliveryDate;
    private String notes;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<PurchaseOrderItemDTO> items;
    /** Pre-calculated total value of the PO (sum of qty * unitPrice for all items) */
    private BigDecimal totalValue;
}
