package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.InventoryRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.InventoryDetailsDTO;
import jakarta.validation.Valid;

import java.util.List;

public interface KitchenInventoryService {
    List<InventoryDetailsDTO> getInventoryAlerts(String userEmail);
    List<InventoryDetailsDTO> getAllInventoryItems(String userEmail);
    void createRequest(@Valid InventoryRequestDTO requestDTO, String userEmail);
    void updateInventoryStock(UpdateStockDTO updateDTO, String userEmail);
}
