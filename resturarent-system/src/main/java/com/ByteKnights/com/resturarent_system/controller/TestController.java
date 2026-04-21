package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.CreateStaffResponse;
import com.ByteKnights.com.resturarent_system.service.StaffService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {

    private final StaffService staffService;

    public TestController(StaffService staffService) {
        this.staffService = staffService;
    }


    // -------STAFF TEST ENDPOINTS-------

    // Any authenticated staff (CHEF, MANAGER, ADMIN, etc.)
    // Both /api/test/staff and /api/staff/test work
    @GetMapping({"/api/test/staff", "/api/staff/test"})
    @PreAuthorize("isAuthenticated()")
    public String staffTest() {
        return "Authenticated staff access granted";
    }

    // -------ROLE-SPECIFIC TEST ENDPOINTS-------


    // Admin and SUPER_ADMIN only
    @GetMapping("/api/test/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String adminTest() {
        return "ADMIN or SUPER_ADMIN access granted";
    }

    // Manager, Admin, SUPER_ADMIN
    @GetMapping("/api/test/manager")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')")
    public String managerTest() {
        return "MANAGER, ADMIN, or SUPER_ADMIN access granted";
    }

    // Receptionist, Admin, SUPER_ADMIN
    @GetMapping("/api/test/receptionist")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN','SUPER_ADMIN')")
    public String receptionistTest() {
        return "RECEPTIONIST, ADMIN, or SUPER_ADMIN access granted";
    }

    // CHEF, Admin, SUPER_ADMIN
    @GetMapping("/api/test/chef")
    @PreAuthorize("hasAnyRole('CHEF','ADMIN','SUPER_ADMIN')")
    public String chefTest() {
        return "CHEF, ADMIN, or SUPER_ADMIN access granted";
    }

    // Delivery, Admin, SUPER_ADMIN
    @GetMapping("/api/test/delivery")
    @PreAuthorize("hasAnyRole('DELIVERY','ADMIN','SUPER_ADMIN')")
    public String deliveryTest() {
        return "DELIVERY, ADMIN, or SUPER_ADMIN access granted";
    }


    // -------RESEND INVITE TEST-------

    // Admin / SUPER_ADMIN can test resend invite
    @PostMapping("/api/test/resend-invite/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public CreateStaffResponse testResendInvite(@PathVariable Long id) {
        return staffService.resendInvite(id);
    }
}
