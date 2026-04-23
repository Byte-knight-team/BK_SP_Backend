package com.ByteKnights.com.resturarent_system.service;

import java.util.List;

import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventorySummaryDTO;

public interface InventoryService {
    /**
     * 1. Retrieves all inventory items for a specific branch.
     * 
     * @param targetBranchId The ID of the branch (provided if Super Admin).
     * @param userId         The ID of the logged-in user to derive their assigned
     *                       branch.
     * @return A list of InventoryItemDTOs formatted for the frontend table.
     */
    List<InventoryItemDTO> getAllItemsByBranch(Long targetBranchId, Long userId);

    /**
     * 2. Calculates and aggregates dashboard metrics for the inventory page.
     * 
     * This method fetches all inventory items and chef requests for the specified
     * branch, processes the mathematical logic (sums and counts), and packages
     * the results into a single DTO.
     * 
     * @param targetBranchId The ID of the branch (provided if Super Admin).
     * @param userId         The ID of the logged-in user to derive their assigned
     *                       branch.
     * @return InventorySummaryDTO containing the final calculated metrics.
     */
    InventorySummaryDTO getInventorySummary(Long targetBranchId, Long userId);
}
