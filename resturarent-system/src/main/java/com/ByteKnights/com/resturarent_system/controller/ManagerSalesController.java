package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerSalesSummaryDTO;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.ManagerSalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales/manager")
@RequiredArgsConstructor
public class ManagerSalesController {

    private final ManagerSalesService managerSalesService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ManagerSalesSummaryDTO>> getSalesSummary(
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {
        
        ManagerSalesSummaryDTO summary = managerSalesService.getSalesSummary(branchId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Sales summary retrieved successfully", summary));
    }
}
