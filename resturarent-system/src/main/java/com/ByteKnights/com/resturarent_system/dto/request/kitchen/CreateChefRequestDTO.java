package com.ByteKnights.com.resturarent_system.dto.request.kitchen;

import com.ByteKnights.com.resturarent_system.entity.ChefRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateChefRequestDTO {
    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotBlank(message = "Unit is required")
    private String unit;

    @Positive(message = "Requested quantity must be greater than zero")
    private BigDecimal requestedQuantity;

    private String chefNote;

    @NotNull(message = "Request type is required")
    private ChefRequestType requestType;
}

