package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.RoleSummaryResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateRoleRequest;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/roles")
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<RoleSummaryResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoleSummaries());
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RoleSummaryResponse> getRoleSummary(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getRoleSummaryById(roleId));
    }

    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Set<String>> getPermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getPermissionsOfRole(roleId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> createRole(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name") != null ? payload.get("name").toString() : null;
        String desc = payload.get("description") != null ? payload.get("description").toString() : null;

        /*
            baseSalary is optional.
            If it is not sent, RoleService will save it as 0.00.
        */
        BigDecimal baseSalary = parseBigDecimal(payload.get("baseSalary"));

        return ResponseEntity.ok(roleService.createRole(name, desc, baseSalary));
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody Set<String> permissionNames) {

        Role role = roleService.assignPermissionsToRole(roleId, permissionNames);
        return ResponseEntity.ok(role);
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> updateRole(@PathVariable Long roleId,
                                           @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(roleId, request));
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
    }

    /*
        Converts incoming JSON number/string values into BigDecimal safely.

        Accepted:
        "baseSalary": 60000
        "baseSalary": "60000"
    */
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Invalid base salary value");
        }
    }
}