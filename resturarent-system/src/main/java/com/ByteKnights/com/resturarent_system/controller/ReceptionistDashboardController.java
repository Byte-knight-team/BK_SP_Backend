package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderCountByTypeDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistRevenueByTypeDTO;
import com.ByteKnights.com.resturarent_system.service.ReceptionistDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/receptionist/dashboard")
@CrossOrigin
@RequiredArgsConstructor
public class ReceptionistDashboardController {

    private final ReceptionistDashboardService receptionistDashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getDashboardStats(Principal principal) {
        ReceptionistDashboardStatsDTO stats =
                receptionistDashboardService.getDashboardStats(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", stats), HttpStatus.OK);
    }

    @GetMapping("/revenue-by-type")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getRevenueByType(Principal principal) {
        List<ReceptionistRevenueByTypeDTO> revenue =
                receptionistDashboardService.getRevenueByType(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", revenue), HttpStatus.OK);
    }

    @GetMapping("/order-counts-by-type")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getOrderCountsByType(Principal principal) {
        ReceptionistOrderCountByTypeDTO counts =
                receptionistDashboardService.getOrderCountsByType(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", counts), HttpStatus.OK);
    }
}
