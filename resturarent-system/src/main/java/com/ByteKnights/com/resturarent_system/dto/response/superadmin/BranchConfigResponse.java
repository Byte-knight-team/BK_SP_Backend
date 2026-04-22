package com.ByteKnights.com.resturarent_system.dto.response.superadmin;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BranchConfigResponse {

    private Long id;
    private Long branchId;
    private String branchName;
    private BigDecimal deliveryFee;
    private boolean deliveryEnabled;
    private boolean pickupEnabled;
    private boolean dineInEnabled;
    private boolean branchActiveForOrders;
    private LocalDateTime updatedAt;
}