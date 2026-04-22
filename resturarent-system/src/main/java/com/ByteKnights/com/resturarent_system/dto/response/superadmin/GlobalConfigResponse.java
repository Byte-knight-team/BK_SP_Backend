package com.ByteKnights.com.resturarent_system.dto.response.superadmin;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class GlobalConfigResponse {

    private Long id;
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
    private LocalDateTime updatedAt;
}