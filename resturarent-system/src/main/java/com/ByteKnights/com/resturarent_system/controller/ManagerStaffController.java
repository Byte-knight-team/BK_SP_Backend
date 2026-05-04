package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerStaffSummaryDTO;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.ManagerStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager/staff")
@RequiredArgsConstructor
public class ManagerStaffController {

    private final ManagerStaffService managerStaffService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('VIEW_STAFF')")
    public ResponseEntity<ManagerStaffSummaryDTO> getStaffSummary(
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {
        
        return ResponseEntity.ok(managerStaffService.getStaffSummary(branchId, userDetails.getUser().getId()));
    }
}
