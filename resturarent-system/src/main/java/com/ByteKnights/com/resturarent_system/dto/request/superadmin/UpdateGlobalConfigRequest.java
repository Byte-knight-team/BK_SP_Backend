package com.ByteKnights.com.resturarent_system.dto.request.superadmin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateGlobalConfigRequest {

    @NotNull
    private Boolean taxEnabled;

    @NotNull
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal taxPercentage;

    @NotNull
    private Boolean serviceChargeEnabled;

    @NotNull
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal serviceChargePercentage;

    @NotNull
    private Boolean loyaltyEnabled;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal pointsPerAmount;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amountPerPoint;

    @NotNull
    @Min(0)
    private Integer minPointsToRedeem;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal valuePerPoint;

    @NotNull
    @Min(0)
    private Integer orderCancelWindowMinutes;
}