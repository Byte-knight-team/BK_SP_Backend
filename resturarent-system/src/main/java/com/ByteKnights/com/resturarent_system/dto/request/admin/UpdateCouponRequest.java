package com.ByteKnights.com.resturarent_system.dto.request.admin;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateCouponRequest {

    private String description;

    @NotNull(message = "Discount value is required")
    @Min(value = 0, message = "Discount value must be zero or greater")
    private BigDecimal discountValue;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be strictly in the future")
    private LocalDateTime expirationDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;
    
    private LocalDateTime startDate;
}
