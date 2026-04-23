package com.ByteKnights.com.resturarent_system.service;

import java.util.List;

import com.ByteKnights.com.resturarent_system.dto.request.inventory.CreateInventoryItemRequest;
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

    /**
     * 3. Adds a new inventory item to the specified branch.
     * 
     * This method handles the creation of a new item, mapping the request DTO
     * to the entity and persisting it to the database.
     * 
     * @param request        The DTO containing the new item details.
     * @param targetBranchId The resolved ID of the branch where the item will be
     *                       added.
     * @return InventoryItemDTO The newly created item, formatted for the frontend.
     */
    InventoryItemDTO addInventoryItem(CreateInventoryItemRequest request, Long targetBranchId, Long userId);

}
