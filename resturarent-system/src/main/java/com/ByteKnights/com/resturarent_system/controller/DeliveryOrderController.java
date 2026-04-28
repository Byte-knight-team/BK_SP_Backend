package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.DeliveryOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery/orders")
@RequiredArgsConstructor
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;

    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<List<DeliveryOrderDTO>>> getAssignedOrders(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        List<DeliveryOrderDTO> orders = deliveryOrderService.getAssignedOrders(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Assigned orders retrieved successfully", orders));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<DeliveryOrderDTO>> getActiveOrder(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        return deliveryOrderService.getActiveOrder(principal.getUser().getId())
                .map(order -> ResponseEntity.ok(ApiResponse.success("Active order retrieved successfully", order)))
                .orElse(ResponseEntity.ok(ApiResponse.success("No active order found", null)));
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        deliveryOrderService.acceptOrder(orderId, principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Order accepted successfully", null));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        String reason = request.getOrDefault("reason", "No reason provided");
        deliveryOrderService.rejectOrder(orderId, principal.getUser().getId(), reason);
        return ResponseEntity.ok(ApiResponse.success("Order rejected successfully", null));
    }
}
