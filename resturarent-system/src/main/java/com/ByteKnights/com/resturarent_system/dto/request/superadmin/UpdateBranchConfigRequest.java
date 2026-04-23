package com.ByteKnights.com.resturarent_system.dto.request.superadmin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateBranchConfigRequest {

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal deliveryFee;

    @NotNull
    private Boolean deliveryEnabled;

    @NotNull
    private Boolean pickupEnabled;

    @NotNull
    private Boolean dineInEnabled;

    @NotNull
    private Boolean branchActiveForOrders;
}