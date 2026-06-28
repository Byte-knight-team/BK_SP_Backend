package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.SuperAdminCustomerResponse;
import com.ByteKnights.com.resturarent_system.service.SuperAdminCustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/customers")
public class SuperAdminCustomerController {

    private final SuperAdminCustomerService superAdminCustomerService;

    public SuperAdminCustomerController(SuperAdminCustomerService superAdminCustomerService) {
        this.superAdminCustomerService = superAdminCustomerService;
    }

    /*
     * SUPER_ADMIN only.
     * Get all customer accounts.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SuperAdminCustomerResponse>>> getAllCustomers() {
        List<SuperAdminCustomerResponse> customers = superAdminCustomerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success("Customers loaded successfully", customers));
    }

    /*
     * SUPER_ADMIN only.
     * View one customer account.
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<SuperAdminCustomerResponse>> getCustomerById(
            @PathVariable Long customerId) {
        SuperAdminCustomerResponse customer = superAdminCustomerService.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer loaded successfully", customer));
    }

    /*
     * SUPER_ADMIN only.
     * Activate customer account.
     */
    @PatchMapping("/{customerId}/activate")
    public ResponseEntity<ApiResponse<SuperAdminCustomerResponse>> activateCustomer(
            @PathVariable Long customerId) {
        SuperAdminCustomerResponse customer = superAdminCustomerService.activateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer account activated successfully", customer));
    }

    /*
     * SUPER_ADMIN only.
     * Deactivate customer account.
     */
    @PatchMapping("/{customerId}/deactivate")
    public ResponseEntity<ApiResponse<SuperAdminCustomerResponse>> deactivateCustomer(
            @PathVariable Long customerId) {
        SuperAdminCustomerResponse customer = superAdminCustomerService.deactivateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer account deactivated successfully", customer));
    }
}