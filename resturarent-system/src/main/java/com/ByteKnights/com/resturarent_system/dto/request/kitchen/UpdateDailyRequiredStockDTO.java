package com.ByteKnights.com.resturarent_system.dto.request.kitchen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDailyRequiredStockDTO {

    @NotBlank(message = "Item name is required")
    private String itemName;

    @PositiveOrZero(message = "Daily required stock cannot be negative")
    private BigDecimal newDailyRequiredStock;
}
