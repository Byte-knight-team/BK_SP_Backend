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
public class SystemConfigCacheDto implements Serializable {

    private Long id;
    
    // Storing only the ID instead of the full Branch entity
    private Long deliveryBranchId;
    
    // Tax Configuration
    private boolean taxEnabled;
    private BigDecimal taxPercentage;
    
    // Service Charge Configuration
    private boolean serviceChargeEnabled;
    private BigDecimal serviceChargePercentage;
    
    // Loyalty Configuration
    private boolean loyaltyEnabled;
    private BigDecimal pointsPerAmount;
    private BigDecimal amountPerPoint;
    private Integer minPointsToRedeem;
    private BigDecimal valuePerPoint;
    
    // Order Configuration
    private Integer orderCancelWindowMinutes;
    
    private LocalDateTime updatedAt;
}
