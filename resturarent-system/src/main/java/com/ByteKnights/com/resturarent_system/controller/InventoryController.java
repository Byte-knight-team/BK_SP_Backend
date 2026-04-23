package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventorySummaryDTO;
import com.ByteKnights.com.resturarent_system.service.InventoryService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;

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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'CHEF')")
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
