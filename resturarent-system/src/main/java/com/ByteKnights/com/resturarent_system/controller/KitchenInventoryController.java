package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.InventoryRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.InventoryDetailsDTO;
import com.ByteKnights.com.resturarent_system.service.KitchenInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kitchen")
@CrossOrigin
@RequiredArgsConstructor
public class KitchenInventoryController {

    private final KitchenInventoryService kitchenInventoryService;

    @GetMapping("/inventory-alerts")
    @PreAuthorize("hasAuthority('KITCHEN_INVENTORY_VIEW')")
    public ResponseEntity<StandardResponse> getInventoryAlerts(Principal principal) {
        List<InventoryDetailsDTO> alerts = kitchenInventoryService.getInventoryAlerts(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", alerts), HttpStatus.OK);
    }

    @GetMapping("/inventory/all")
    @PreAuthorize("hasAuthority('KITCHEN_INVENTORY_VIEW')")
    public ResponseEntity<StandardResponse> getAllInventory(Principal principal) {
        List<InventoryDetailsDTO> items = kitchenInventoryService.getAllInventoryItems(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", items), HttpStatus.OK);
    }

    @PostMapping("/inventory/request")
    @PreAuthorize("hasAuthority('KITCHEN_INVENTORY_REQUEST')")
    public ResponseEntity<StandardResponse> createRequest(
            @Valid @RequestBody InventoryRequestDTO requestDTO,
            Principal principal) {
        kitchenInventoryService.createRequest(requestDTO, principal.getName());
        return new ResponseEntity<>(
                new StandardResponse(201, "Inventory request submitted successfully!", null),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/inventory/update")
    @PreAuthorize("hasAuthority('KITCHEN_INVENTORY_UPDATE')")
    public ResponseEntity<StandardResponse> updateInventoryStock(
            @Valid @RequestBody UpdateStockDTO updateDTO,
            Principal principal) {
        kitchenInventoryService.updateInventoryStock(updateDTO, principal.getName());
        return new ResponseEntity<>(
                new StandardResponse(200, "Stock updated successfully!", null),
                HttpStatus.OK
        );
    }
}
