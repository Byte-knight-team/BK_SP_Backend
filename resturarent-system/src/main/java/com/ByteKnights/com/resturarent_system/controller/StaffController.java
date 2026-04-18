package com.byteknights.com.resturarent_system.controller;

import com.byteknights.com.resturarent_system.dto.CreateStaffRequest;
import com.byteknights.com.resturarent_system.dto.CreateStaffResponse;
import com.byteknights.com.resturarent_system.service.StaffService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public CreateStaffResponse createStaff(@RequestBody CreateStaffRequest request) {
        return staffService.createStaff(request);
    }

    @PostMapping("/{id}/resend-invite")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public CreateStaffResponse resendInvite(@PathVariable Long id) {
        return staffService.resendInvite(id);
    }
}