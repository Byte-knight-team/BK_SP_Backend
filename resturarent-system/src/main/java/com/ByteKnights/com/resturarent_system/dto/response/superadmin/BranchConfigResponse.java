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

    /*
     * Delivery configuration
     */

    private BigDecimal deliveryFee;
    private BigDecimal deliveryFeePerKm;
    private Double maxDeliveryRadiusKm;

    private boolean deliveryEnabled;
    private boolean pickupEnabled;
    private boolean dineInEnabled;
    private boolean branchActiveForOrders;

    /*
     * Reservation configuration
     */

    private BigDecimal reservationFeePerHour;
    private BigDecimal reservationHandlingFee;

    private Integer reservationPaymentWindowMinutes;
    private Integer reservationMinLeadHours;
    private Integer reservationMaxGuestCount;

    private boolean reservationsEnabled;

    private LocalDateTime updatedAt;
}