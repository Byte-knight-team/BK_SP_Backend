package com.ByteKnights.com.resturarent_system.dto.response.superadmin;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class EffectiveBranchConfigResponse {

    private Long branchId;
    private String branchName;

    private boolean taxEnabled;
    private BigDecimal taxPercentage;

    private boolean serviceChargeEnabled;
    private BigDecimal serviceChargePercentage;

    private boolean loyaltyEnabled;
    private BigDecimal pointsPerAmount;
    private BigDecimal amountPerPoint;
    private Integer minPointsToRedeem;
    private BigDecimal valuePerPoint;

    private Integer orderCancelWindowMinutes;

    private BigDecimal deliveryFee;
    private boolean deliveryEnabled;
    private boolean pickupEnabled;
    private boolean dineInEnabled;
    private boolean branchActiveForOrders;
}