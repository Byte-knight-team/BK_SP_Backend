package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerDashboardSummaryDTO;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.ManagerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/manager")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ManagerDashboardSummaryDTO>> getDashboardSummary(
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {
            
        ManagerDashboardSummaryDTO summary = managerDashboardService.getDashboardSummary(branchId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved successfully", summary));
    }
}
