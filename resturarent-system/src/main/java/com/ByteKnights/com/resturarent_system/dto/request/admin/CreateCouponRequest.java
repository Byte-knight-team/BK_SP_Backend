package com.ByteKnights.com.resturarent_system.dto.request.admin;

import com.ByteKnights.com.resturarent_system.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateCouponRequest {
    @NotBlank
    private String code;
    
    private String description;
    
    @NotNull
    private DiscountType discountType;
    
    @NotNull
    private BigDecimal discountValue;
    
    private BigDecimal minOrderAmount;
    
    private BigDecimal maxDiscount;
    
    @NotNull
    private LocalDate startDate;
    
    @NotNull
    private LocalDate expirationDate;
    
    private Integer usageLimit;
}
