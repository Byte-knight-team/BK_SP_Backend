package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventorySummaryDTO;

import java.util.List;

/**
 * Service interface for Inventory Management.
 * 
 * We use an interface to define the "contract" of what the inventory system can
 * do,
 * without exposing how it actually does it. This promotes loose coupling and
 * makes
 * the Controller completely independent of the database implementation.
 */

public interface InventoryService {
    /**
     * 1. Retrieves all inventory items for a specific branch.
     * 
     * @param branchId The ID of the branch requesting the inventory.
     * @return A list of InventoryItemDTOs formatted for the frontend table.
     */
    List<InventoryItemDTO> getAllItemsByBranch(Long branchId);

    /**
     * 2. Calculates and aggregates dashboard metrics for the inventory page.
     * 
     * This method fetches all inventory items and chef requests for the specified
     * branch, processes the mathematical logic (sums and counts), and packages
     * the results into a single DTO.
     * 
     * @param branchId The ID of the branch to calculate metrics for.
     * @return InventorySummaryDTO containing the final calculated metrics.
     */
    InventorySummaryDTO getInventorySummary(Long branchId);

}
