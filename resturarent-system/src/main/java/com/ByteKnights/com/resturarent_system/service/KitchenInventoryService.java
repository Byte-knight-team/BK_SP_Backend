package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.InventoryRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateDailyRequiredStockDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.InventoryDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.PagedResponse;
import com.ByteKnights.com.resturarent_system.entity.InventoryItem;
import jakarta.validation.Valid;

import java.util.List;

public interface KitchenInventoryService {
    List<InventoryDetailsDTO> getInventoryAlerts(String userEmail);
    List<InventoryDetailsDTO> getAllInventoryItems(String userEmail);
    void createRequest(@Valid InventoryRequestDTO requestDTO, String userEmail);
    void updateInventoryStock(UpdateStockDTO updateDTO, String userEmail);
    void updateDailyRequiredStock(UpdateDailyRequiredStockDTO updateDTO, String userEmail);
    PagedResponse<ChefRequestDTO> getMyRequests(String userEmail, int page, int size, String date, String status);

    /**
     * Checks an inventory item's current stock level and broadcasts a global kitchen
     * notification if it just dipped into LOW/CRITICAL (or clears the flag once restocked).
     * Called from every place stock quantity decreases.
     */
    void checkAndNotifyStockLevel(InventoryItem item);
}
