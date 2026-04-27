package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerDriverSummaryDTO;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.ManagerDriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drivers/manager")
@RequiredArgsConstructor
public class ManagerDriverController {

    private final ManagerDriverService managerDriverService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ManagerDriverSummaryDTO>> getDriverSummary(
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {
        
        ManagerDriverSummaryDTO summary = managerDriverService.getDriverSummary(branchId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Driver summary retrieved successfully", summary));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignDriver(
            @RequestParam Long orderId,
            @RequestParam Long driverId) {
        
        managerDriverService.assignDriver(orderId, driverId);
        return ResponseEntity.ok(ApiResponse.success("Driver assigned successfully", null));
    }
}
