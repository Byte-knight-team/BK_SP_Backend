package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.DeliveryStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/delivery/status")
@RequiredArgsConstructor
public class DeliveryStatusController {

    private final DeliveryStatusService deliveryStatusService;

    @GetMapping
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal JwtUserPrincipal principal) {
        boolean isOnline = deliveryStatusService.getOnlineStatus(principal.getUser().getId());
        return ResponseEntity.ok(Map.of("isOnline", isOnline));
    }

    @PostMapping("/toggle")
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
