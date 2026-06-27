package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistRevenuePointDTO;
import com.ByteKnights.com.resturarent_system.service.ReceptionistDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

// Same structure as KitchenDashboardController — two GET endpoints under /dashboard
@RestController
@RequestMapping("/api/v1/receptionist/dashboard")
@CrossOrigin
@RequiredArgsConstructor
public class ReceptionistDashboardController {

    private final ReceptionistDashboardService receptionistDashboardService;

    // GET /api/v1/receptionist/dashboard/stats
    // Returns KPI counts + pipeline breakdown for today
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getDashboardStats(Principal principal) {
        ReceptionistDashboardStatsDTO stats =
                receptionistDashboardService.getDashboardStats(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", stats), HttpStatus.OK);
    }

    // GET /api/v1/receptionist/dashboard/revenue
    // Returns last 7 days of collected payments for the revenue bar chart
    @GetMapping("/revenue")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getLast7DaysRevenue(Principal principal) {
        List<ReceptionistRevenuePointDTO> revenue =
                receptionistDashboardService.getLast7DaysRevenue(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", revenue), HttpStatus.OK);
    }
}
