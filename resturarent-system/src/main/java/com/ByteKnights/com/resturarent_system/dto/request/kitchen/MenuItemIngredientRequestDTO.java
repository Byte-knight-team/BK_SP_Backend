package com.ByteKnights.com.resturarent_system.dto.request.kitchen;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

// Request body for saving the full ingredient list of a menu item
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemIngredientRequestDTO {

    // Full list of ingredients to save (replaces any existing ones)
    @NotNull(message = "Ingredients list is required")
    private List<IngredientEntry> ingredients;

    // Each entry links one inventory item with a required quantity
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientEntry {

        @NotNull(message = "Inventory item ID is required")
        private Long inventoryItemId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
        private BigDecimal quantityRequired;
    }
}
