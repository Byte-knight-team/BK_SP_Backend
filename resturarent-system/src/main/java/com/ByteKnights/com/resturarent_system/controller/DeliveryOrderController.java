package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.DeliveryOrderService;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller responsible for handling all HTTP requests related to Delivery Operations.
 * 
 * This controller provides endpoints for delivery personnel (drivers) to manage their
 * assigned tasks, track active deliveries, and update the status of orders as they
 * progress from acceptance to final delivery.
 */
@RestController
@RequestMapping("/api/delivery/orders")
@RequiredArgsConstructor
public class DeliveryOrderController {

    /**
     * INJECTED SERVICE
     * 
     * We inject the DeliveryOrderService INTERFACE here, not the implementation class.
     * This ensures our controller is completely decoupled from database logic.
     * The controller's only job is to receive the HTTP request, pass the data to
     * the service, and return the service's response back to the frontend.
     */
    private final DeliveryOrderService deliveryOrderService;

    /**
     * Endpoint to fetch all orders currently assigned to the logged-in delivery staff member.
     * 
     * Path: GET /api/delivery/orders/assigned
     * 
     * @param principal The authenticated user principal (driver).
     * @return 200 OK with a list of assigned DeliveryOrderDTOs.
     */
    @GetMapping("/assigned")
    @PreAuthorize("hasAuthority('VIEW_DELIVERY')")
    public ResponseEntity<ApiResponse<List<DeliveryOrderDTO>>> getAssignedOrders(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        List<DeliveryOrderDTO> orders = deliveryOrderService.getAssignedOrders(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Assigned orders retrieved successfully", orders));
    }

    /**
     * Endpoint to retrieve the single active delivery task for the logged-in driver.
     * 
     * Path: GET /api/delivery/orders/active
     * 
     * @param principal The authenticated user principal.
     * @return 200 OK with the active DeliveryOrderDTO, or null if no active task exists.
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('VIEW_DELIVERY')")
    public ResponseEntity<ApiResponse<DeliveryOrderDTO>> getActiveOrder(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        return deliveryOrderService.getActiveOrder(principal.getUser().getId())
                .map(order -> ResponseEntity.ok(ApiResponse.success("Active order retrieved successfully", order)))
                .orElse(ResponseEntity.ok(ApiResponse.success("No active order found", null)));
    }

    /**
     * Endpoint to accept a newly assigned order.
     * 
     * Path: POST /api/delivery/orders/{orderId}/accept
     * 
     * @param orderId   The unique ID of the order.
     * @param principal The authenticated user principal.
     * @return 200 OK on successful acceptance.
     */
    @PostMapping("/{orderId}/accept")
    @PreAuthorize("hasAuthority('UPDATE_DELIVERY_STATUS')")
    public ResponseEntity<ApiResponse<Void>> acceptOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        deliveryOrderService.acceptOrder(orderId, principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Order accepted successfully", null));
    }

    /**
     * Endpoint to reject an assigned order with a specific reason.
     * 
     * Path: POST /api/delivery/orders/{orderId}/reject
     * 
     * @param orderId   The unique ID of the order.
     * @param request   Map containing the "reason" for rejection.
     * @param principal The authenticated user principal.
     * @return 200 OK on successful rejection.
     */
    @PostMapping("/{orderId}/reject")
    @PreAuthorize("hasAuthority('UPDATE_DELIVERY_STATUS')")
    public ResponseEntity<ApiResponse<Void>> rejectOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        String reason = request.getOrDefault("reason", "No reason provided");
        deliveryOrderService.rejectOrder(orderId, principal.getUser().getId(), reason);
        return ResponseEntity.ok(ApiResponse.success("Order rejected successfully", null));
    }

    /**
     * Endpoint to update the lifecycle status of an active delivery.
     * 
     * Path: POST /api/delivery/orders/{orderId}/status
     * 
     * @param orderId   The unique ID of the order.
     * @param request   Map containing the new "status" (e.g., OUT_FOR_DELIVERY, DELIVERED).
     * @param principal The authenticated user principal.
     * @return 200 OK on successful status update.
     */
    @PostMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('UPDATE_DELIVERY_STATUS')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        DeliveryStatus status = DeliveryStatus.valueOf(request.get("status"));
                
        deliveryOrderService.updateStatus(orderId, principal.getUser().getId(), status);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", null));
    }
}
