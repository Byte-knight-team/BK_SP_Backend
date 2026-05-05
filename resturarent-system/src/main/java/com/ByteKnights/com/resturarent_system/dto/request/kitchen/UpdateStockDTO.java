package com.ByteKnights.com.resturarent_system.dto.request.kitchen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateStockDTO {

    // this field cannot be empty
    @NotBlank(message = "Item name is required")
    private String itemName;

    // use PositiveOrZero because stock can be 0, but it cannot be negative
    @PositiveOrZero(message = "Quantity cannot be negative")
    private BigDecimal newQuantity;
}
