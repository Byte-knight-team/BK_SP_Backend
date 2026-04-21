package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryItemDTO;
import com.ByteKnights.com.resturarent_system.service.InventoryService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for handling all HTTP requests related to Inventory
 * Management.
 * 
 * The @RestController annotation tells Spring that this class is an API
 * controller,
 * and it will automatically convert all returned Java objects directly into
 * JSON
 * for the frontend to consume.
 * 
 * The @RequestMapping("/api/inventory") annotation means that every endpoint we
 * build
 * inside this class will automatically start with
 * "http://localhost:8080/api/inventory".
 */

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    /**
     * INJECTED SERVICE
     * 
     * We inject the InventoryService INTERFACE here, not the implementation class.
     * This ensures our controller is completely decoupled from database logic.
     * The controller's only job is to receive the HTTP request, pass the data to
     * the
     * service, and return the service's response back to the frontend.
     */
    private final InventoryService inventoryService;

    /**
     * Endpoint to fetch all inventory items for a specific branch.
     * 
     * Path: GET /api/inventory/items?branchId={id}
     * 
     * @param branchId Retrieved from the URL query parameter.
     * @return 200 OK with a list of InventoryItemDTOs in the response body.
     */
    @GetMapping("/items")
    public ResponseEntity<List<InventoryItemDTO>> getAllItemsByBranch(
            @RequestParam Long branchId) {

        // Ask the service layer to do the heavy lifting
        List<InventoryItemDTO> items = inventoryService.getAllItemsByBranch(branchId);

        // Wrap the result in a 200 OK HTTP Response and send it back to the frontend
        return ResponseEntity.ok(items);
    }

}
