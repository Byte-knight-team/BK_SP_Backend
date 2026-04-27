package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.request.inventory.CreateInventoryItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.inventory.RemoveInventoryStockRequest;
import com.ByteKnights.com.resturarent_system.dto.request.inventory.RestockInventoryItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.inventory.UpdateInventoryItemRequest;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryLogDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventorySummaryDTO;
import com.ByteKnights.com.resturarent_system.service.InventoryService;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'CHEF')")
    public ResponseEntity<List<InventoryItemDTO>> getAllItemsByBranch(
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long targetBranchId = resolveTargetBranchId(branchId, principal);
        Long userId = principal.getUser().getId();

        // Ask the service layer to do the heavy lifting
        List<InventoryItemDTO> items = inventoryService.getAllItemsByBranch(targetBranchId, userId);

        // Wrap the result in a 200 OK HTTP Response and send it back to the frontend
        return ResponseEntity.ok(items);
    }

    /**
     * REST Endpoint to fetch the top dashboard summary metrics.
     * 
     * Path: GET /api/inventory/summary?branchId={id}
     * 
     * This endpoint is called when the Inventory Management page first loads.
     * It returns the aggregated financial values, stock alerts, and pending
     * requests.
     * 
     * @param branchId Retrieved from the URL query parameter.
     * @return 200 OK with the populated InventorySummaryDTO.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<InventorySummaryDTO> getInventorySummary(
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long targetBranchId = resolveTargetBranchId(branchId, principal);
        Long userId = principal.getUser().getId();

        // Ask the service layer to do all the heavy math and database fetching
        InventorySummaryDTO summary = inventoryService.getInventorySummary(targetBranchId, userId);

        // Return the packaged data as a JSON response
        return ResponseEntity.ok(summary);
    }

    /**
     * 3. Adds a new inventory item to the system.
     * 
     * Path: POST /api/inventory/items
     * 
     * This endpoint allows authorized staff to create a new item in the
     * inventory. The branch is determined securely based on the user's
     * profile or provided in the request if the user is a Super Admin.
     * 
     * @param request   The DTO containing the details of the item to be created.
     * @param principal The authenticated user principal.
     * @return 201 Created with the newly created InventoryItemDTO.
     */
    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<InventoryItemDTO> addInventoryItem(
            @Valid @RequestBody CreateInventoryItemRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        // Determine the target branch (Super Admin flexibility vs. Manager strictness)
        Long targetBranchId = resolveTargetBranchId(request.getBranchId(), principal);

        Long userId = principal.getUser().getId(); // Get the user ID

        // Call the service to create the item
        InventoryItemDTO createdItem = inventoryService.addInventoryItem(request, targetBranchId, userId);

        // Return 201 Created along with the new item data
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    /**
     * 4. Restocks an existing inventory item.
     * 
     * Path: PATCH /api/inventory/items/{id}/restock
     * 
     * This endpoint handles stock replenishment for an existing item.
     * 
     * @param id        The ID of the inventory item.
     * @param request   The restock details.
     * @param principal The authenticated user.
     * @return 200 OK with the updated InventoryItemDTO.
     */
    @PatchMapping("/items/{id}/restock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<InventoryItemDTO> restockItem(
            @PathVariable Long id,
            @RequestBody RestockInventoryItemRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long userId = principal.getUser().getId();
        InventoryItemDTO updatedItem = inventoryService.restockItem(id, request, userId);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * 5. Removes stock from an existing inventory item (wastage/damage).
     * 
     * Path: PATCH /api/inventory/items/{id}/remove
     * 
     * This endpoint handles stock reduction due to wastage or damage.
     * 
     * @param id        The ID of the inventory item.
     * @param request   The removal details.
     * @param principal The authenticated user.
     * @return 200 OK with the updated InventoryItemDTO.
     */
    @PatchMapping("/items/{id}/remove")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<InventoryItemDTO> removeStock(
            @PathVariable Long id,
            @RequestBody RemoveInventoryStockRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long userId = principal.getUser().getId();
        InventoryItemDTO updatedItem = inventoryService.removeStock(id, request, userId);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * 6. Corrects or updates an existing inventory item's details.
     * 
     * Path: PUT /api/inventory/items/{id}/correct
     * 
     * This endpoint allows managers to fix errors in item metadata or stock levels.
     * 
     * @param id        The ID of the inventory item.
     * @param request   The corrected details.
     * @param principal The authenticated user.
     * @return 200 OK with the updated InventoryItemDTO.
     */
    @PutMapping("/items/{id}/correct")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<InventoryItemDTO> correctItem(
            @PathVariable Long id,
            @RequestBody UpdateInventoryItemRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long userId = principal.getUser().getId();
        InventoryItemDTO updatedItem = inventoryService.correctItem(id, request, userId);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * 7. Fetches the history of all inventory updates (logs).
     * 
     * Path: GET /api/inventory/logs?branchId={id}
     * 
     * @param branchId Retrieved from the URL query parameter.
     * @param principal The authenticated user.
     * @return 200 OK with the list of InventoryLogDTOs.
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<List<InventoryLogDTO>> getInventoryLogs(
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long targetBranchId = resolveTargetBranchId(branchId, principal);
        Long userId = principal.getUser().getId();

        List<InventoryLogDTO> logs = inventoryService.getInventoryLogs(targetBranchId, userId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Helper method to resolve which branch ID to query.
     * SUPER_ADMINs can query the requested branch. Everyone else gets null
     * (forcing the service to look up their assigned branch).
     */
    private Long resolveTargetBranchId(Long requestedBranchId, JwtUserPrincipal principal) {
        boolean isSuperAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        return isSuperAdmin ? requestedBranchId : null;
    }

}
