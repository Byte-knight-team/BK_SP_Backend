package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.DeliveryStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller responsible for managing the availability status of delivery staff.
 * 
 * This controller provides endpoints to check whether a driver is currently 
 * "Online" (available for assignments) or "Offline", and allows them to
 * toggle this state.
 */
@RestController
@RequestMapping("/api/delivery/status")
@RequiredArgsConstructor
public class DeliveryStatusController {

    /**
     * INJECTED SERVICE
     * 
     * We inject the DeliveryStatusService INTERFACE here, not the implementation class.
     * This ensures our controller is completely decoupled from database logic.
     * The controller's only job is to receive the HTTP request, pass the data to
     * the service, and return the service's response back to the frontend.
     */
    private final DeliveryStatusService deliveryStatusService;

    /**
     * Endpoint to retrieve the current online status of the logged-in driver.
     * 
     * Path: GET /api/delivery/status
     * 
     * @param principal The authenticated user principal (driver).
     * @return 200 OK with a map containing "isOnline" boolean.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_DELIVERY_STATUS')")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal JwtUserPrincipal principal) {
        boolean isOnline = deliveryStatusService.getOnlineStatus(principal.getUser().getId());
        return ResponseEntity.ok(Map.of("isOnline", isOnline));
    }

    /**
     * Endpoint to toggle the online/offline availability of the logged-in driver.
     * 
     * Path: POST /api/delivery/status/toggle
     * 
     * @param principal The authenticated user principal.
     * @param request   Map containing the new "isOnline" status.
     * @return 200 OK with the updated status.
     */
    @PostMapping("/toggle")
    @PreAuthorize("hasAuthority('MANAGE_DELIVERY_STATUS')")
    public ResponseEntity<?> toggleStatus(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody Map<String, Boolean> request) {
        
        boolean online = request.getOrDefault("isOnline", false);
        deliveryStatusService.toggleOnlineStatus(principal.getUser().getId(), online);
        
        return ResponseEntity.ok(Map.of(
                "message", "Status updated successfully",
                "isOnline", online
        ));
    }
}
