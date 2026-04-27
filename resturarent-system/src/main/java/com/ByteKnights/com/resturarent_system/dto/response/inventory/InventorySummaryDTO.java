package com.ByteKnights.com.resturarent_system.dto.response.inventory;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object (DTO) containing all aggregated metrics needed
 * for the top dashboard cards in the Inventory page.
 * 
 * Instead of making the frontend run 3 different API calls to get the
 * total value, low stock alerts, and pending requests separately,
 * we bundle everything into this single object for maximum efficiency.
 */
@Data
@Builder
public class InventorySummaryDTO {

    /** The name of the branch this summary belongs to (e.g., "Colombo Main") */
    private String branch;

    /** The total financial value of all current stock (Quantity * Unit Price) */
    private BigDecimal totalInventoryValue;

    /** The total count of Chef Requests that are currently in 'PENDING' status */
    private int pendingChefDrafts;

    /**
     * The total count of Inventory Items where current quantity <= reorder level
     */
    private int lowStockAlerts;

    /** A list of all pending Chef Requests to populate the Chef Requests card UI */
    private List<ChefRequestDTO> chefRequests;
}
