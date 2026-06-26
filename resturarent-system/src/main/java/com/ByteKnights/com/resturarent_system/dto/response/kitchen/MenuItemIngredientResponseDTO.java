package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Returned when the frontend fetches the ingredient list for a menu item
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemIngredientResponseDTO {

    private Long id;

    // Inventory item details (so frontend can display name and unit)
    private Long inventoryItemId;
    private String inventoryItemName;
    private String unit;

    // How much of this item is needed per serving
    private BigDecimal quantityRequired;
}
