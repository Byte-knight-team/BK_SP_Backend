package com.ByteKnights.com.resturarent_system.dto.response.procurement;

import com.ByteKnights.com.resturarent_system.entity.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PurchaseOrderLogDTO {
    private Long id;
    private Long purchaseOrderId;
    private String poNumber;
    private String vendorName;
    private PurchaseOrderStatus status;
    private String actionByName;
    private LocalDateTime createdAt;
}
