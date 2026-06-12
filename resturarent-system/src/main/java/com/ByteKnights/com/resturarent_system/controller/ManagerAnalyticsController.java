package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.response.manager.analytics.AnalyticsSummaryDTO;
import com.ByteKnights.com.resturarent_system.service.ManagerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for handling Manager Analytics and Reporting requests.
 * Provides endpoints for high-level business intelligence and operational metrics.
 */
@RestController
@RequestMapping("/api/manager/analytics")
@RequiredArgsConstructor
public class ManagerAnalyticsController {

    /**
     * INJECTED SERVICE
     * 
     * We inject the ManagerAnalyticsService INTERFACE here, not the implementation class.
     * This ensures our controller is completely decoupled from database logic.
     * The controller's only job is to receive the HTTP request, pass the data to
     * the service, and return the service's response back to the frontend.
     */
    private final ManagerAnalyticsService analyticsService;

    /**
     * Retrieves a comprehensive analytics summary including revenue, volume, 
     * trends, and channel distribution.
     * 
     * @param branchId  The ID of the branch (required).
     * @param userId    The ID of the user requesting data (for validation).
     * @param startDate Start date for the analysis (defaults to 30 days ago).
     * @param endDate   End date for the analysis (defaults to today).
     * @return ResponseEntity containing AnalyticsSummaryDTO.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<AnalyticsSummaryDTO> getAnalyticsSummary(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);
        
        AnalyticsSummaryDTO summary = analyticsService.getBranchAnalyticsSummary(branchId, userId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}
