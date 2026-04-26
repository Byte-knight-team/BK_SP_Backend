package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateBranchConfigRequest;
import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateGlobalConfigRequest;
import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateOperatingHoursRequest;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.BranchConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.EffectiveBranchConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.GlobalConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.OperatingHourItemResponse;
import com.ByteKnights.com.resturarent_system.service.SystemConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GlobalConfigResponse> getGlobalConfig() {
        return ResponseEntity.ok(systemConfigService.getGlobalConfig());
    }

    @PutMapping("/global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GlobalConfigResponse> updateGlobalConfig(
            @Valid @RequestBody UpdateGlobalConfigRequest request
    ) {
        return ResponseEntity.ok(systemConfigService.updateGlobalConfig(request));
    }

    @GetMapping("/branches/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<BranchConfigResponse> getBranchConfig(@PathVariable Long branchId) {
        return ResponseEntity.ok(systemConfigService.getBranchConfig(branchId));
    }

    @PutMapping("/branches/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<BranchConfigResponse> updateBranchConfig(
            @PathVariable Long branchId,
            @Valid @RequestBody UpdateBranchConfigRequest request
    ) {
        return ResponseEntity.ok(systemConfigService.updateBranchConfig(branchId, request));
    }

    @GetMapping("/branches/{branchId}/operating-hours")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<OperatingHourItemResponse>> getOperatingHours(@PathVariable Long branchId) {
        return ResponseEntity.ok(systemConfigService.getOperatingHours(branchId));
    }

    @PutMapping("/branches/{branchId}/operating-hours")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<OperatingHourItemResponse>> updateOperatingHours(
            @PathVariable Long branchId,
            @Valid @RequestBody UpdateOperatingHoursRequest request
    ) {
        return ResponseEntity.ok(systemConfigService.updateOperatingHours(branchId, request));
    }

    @GetMapping("/branches/{branchId}/effective")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<EffectiveBranchConfigResponse> getEffectiveBranchConfig(@PathVariable Long branchId) {
        return ResponseEntity.ok(systemConfigService.getEffectiveBranchConfig(branchId));
    }
}