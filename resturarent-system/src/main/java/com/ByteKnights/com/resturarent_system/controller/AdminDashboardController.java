package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardOrderFlowResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardRevenuePointResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardSummaryResponse;
import com.ByteKnights.com.resturarent_system.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AdminDashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminDashboardService.getSummary());
    }

    @GetMapping("/order-flow")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AdminDashboardOrderFlowResponse> getOrderFlow() {
        return ResponseEntity.ok(adminDashboardService.getOrderFlowSummary());
    }

    @GetMapping("/revenue-trend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<AdminDashboardRevenuePointResponse>> getRevenueTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(adminDashboardService.getRevenueTrend(days));
    }
}
