package com.ByteKnights.com.resturarent_system.dto.response.manager.procurement;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GoodsReceiptNoteDTO {
    private Long id;
    private Long purchaseOrderId;
    private String poNumber;
    private String vendorName;
    private String invoiceReference;
    private String receivedByName;
    private LocalDateTime receivedAt;
    private String notes;
    private List<GrnLineItemDTO> items;
    /** True if any line item has a discrepancy between ordered and received quantities */
    private boolean hasDiscrepancies;
}
