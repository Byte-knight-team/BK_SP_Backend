package com.byteknights.com.resturarent_system.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/admin/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminTest() {
        return "ADMIN access granted";
    }

    @GetMapping("/api/manager/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String managerTest() {
        return "MANAGER or ADMIN access granted";
    }

    @GetMapping("/api/staff/test")
    public String staffTest() {
        return "Authenticated staff access granted";
    }
}