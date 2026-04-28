package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerStaffSummaryDTO;
import com.ByteKnights.com.resturarent_system.service.ManagerStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager/staff")
@RequiredArgsConstructor
public class ManagerStaffController {

    private final ManagerStaffService managerStaffService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('MANAGER') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ManagerStaffSummaryDTO> getStaffSummary(
            @RequestParam(required = false) Long branchId,
            @RequestAttribute("userId") Long userId) {
        
        return ResponseEntity.ok(managerStaffService.getStaffSummary(branchId, userId));
    }
}
