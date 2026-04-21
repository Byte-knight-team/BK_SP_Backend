package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryItemDTO;
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
     * Retrieves all inventory items for a specific branch.
     * 
     * @param branchId The ID of the branch requesting the inventory.
     * @return A list of InventoryItemDTOs formatted for the frontend table.
     */
    List<InventoryItemDTO> getAllItemsByBranch(Long branchId);
}
