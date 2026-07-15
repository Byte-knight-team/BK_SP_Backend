package com.ByteKnights.com.resturarent_system.dto.cache;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class BranchConfigCacheDto implements Serializable {

    private Long id;
    private Long branchId;
    
    // Delivery Configuration
    private BigDecimal deliveryFee;
    private BigDecimal deliveryFeePerKm;
    private Double maxDeliveryRadiusKm;
    
    private boolean deliveryEnabled;
    private boolean pickupEnabled;
    private boolean dineInEnabled;
    private boolean branchActiveForOrders;

    // Reservation Configuration
    private BigDecimal reservationFeePerHour;
    private BigDecimal reservationHandlingFee;
    private Integer reservationPaymentWindowMinutes;
    private Integer reservationMinLeadHours;
    private Integer reservationMaxGuestCount;
    private boolean reservationsEnabled;

    private LocalDateTime updatedAt;
}
