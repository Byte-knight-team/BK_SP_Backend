package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.CreateStaffRequest;
import com.ByteKnights.com.resturarent_system.dto.CreateStaffResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateStaffRequest;
import com.ByteKnights.com.resturarent_system.service.StaffService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    /*
     * Dynamic RBAC test endpoint.
     * SUPER_ADMIN can always create staff.
     * Other allowed admin side users must have CREATE_STAFF privilege.
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CREATE_STAFF')")
    public CreateStaffResponse createStaff(@RequestBody CreateStaffRequest request) {
        return staffService.createStaff(request);
    }

    @PostMapping("/{id}/resend-invite")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CREATE_STAFF')")
    public CreateStaffResponse resendInvite(@PathVariable Long id) {
        return staffService.resendInvite(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<StaffResponse> getAllStaff() {
        return staffService.getAllStaff();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public StaffResponse getStaffById(@PathVariable Long id) {
        return staffService.getStaffById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public StaffResponse updateStaff(@PathVariable Long id,
                                     @RequestBody UpdateStaffRequest request) {
        return staffService.updateStaff(id, request);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public StaffResponse activateStaff(@PathVariable Long id) {
        return staffService.activateStaff(id);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public StaffResponse deactivateStaff(@PathVariable Long id) {
        return staffService.deactivateStaff(id);
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<StaffResponse> getStaffByBranch(@PathVariable Long branchId) {
        return staffService.getStaffByBranch(branchId);
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<StaffResponse> getStaffByRole(@PathVariable String roleName) {
        return staffService.getStaffByRole(roleName);
    }
}