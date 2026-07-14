package com.ByteKnights.com.resturarent_system.dto.request.superadmin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateBranchConfigRequest {

    /*
     * Delivery configuration
     */

    @NotNull
    @DecimalMin(
            value = "0.00",
            message = "Delivery fee cannot be negative"
    )
    private BigDecimal deliveryFee;

    @NotNull
    @DecimalMin(
            value = "0.00",
            message = "Delivery fee per kilometre cannot be negative"
    )
    private BigDecimal deliveryFeePerKm;

    @NotNull
    @DecimalMin(
            value = "0.00",
            message = "Maximum delivery radius cannot be negative"
    )
    private Double maxDeliveryRadiusKm;

    @NotNull
    private Boolean deliveryEnabled;

    @NotNull
    private Boolean pickupEnabled;

    @NotNull
    private Boolean dineInEnabled;

    @NotNull
    private Boolean branchActiveForOrders;

    /*
     * Reservation configuration
     *
     * These fields are optional for backward compatibility.
     *
     * Existing clients that send only delivery/order settings will
     * not erase the branch's existing reservation configuration.
     */

    @DecimalMin(
            value = "0.00",
            message = "Reservation fee per hour cannot be negative"
    )
    private BigDecimal reservationFeePerHour;

    @DecimalMin(
            value = "0.00",
            message = "Reservation handling fee cannot be negative"
    )
    private BigDecimal reservationHandlingFee;

    @Min(
            value = 1,
            message = "Reservation payment window must be at least 1 minute"
    )
    private Integer reservationPaymentWindowMinutes;

    @Min(
            value = 0,
            message = "Reservation minimum lead time cannot be negative"
    )
    private Integer reservationMinLeadHours;

    @Min(
            value = 1,
            message = "Reservation maximum guest count must be at least 1"
    )
    private Integer reservationMaxGuestCount;

    private Boolean reservationsEnabled;
}