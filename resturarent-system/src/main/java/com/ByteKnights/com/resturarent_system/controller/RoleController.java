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

    /*
        SUPER_ADMIN and ADMIN can read role summaries.
    */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<RoleSummaryResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoleSummaries());
    }

    /*
        Role detail view is safe as read only access
    */
    @GetMapping("/{roleId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<RoleSummaryResponse> getRoleSummary(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getRoleSummaryById(roleId));
    }

    /*
        Permissions can be viewed by ADMIN, but only SUPER_ADMIN can update them.
    */
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Set<String>> getPermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getPermissionsOfRole(roleId));
    }

    /*
        Only SUPER_ADMIN can create new roles.
    */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> createRole(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name") != null
                ? payload.get("name").toString()
                : null;

        String description = payload.get("description") != null
                ? payload.get("description").toString()
                : null;

        /*
            baseSalary is optional and can be saved as 0.00 if not sent
        */
        BigDecimal baseSalary = parseBigDecimal(payload.get("baseSalary"));

        Role createdRole = roleService.createRole(name, description, baseSalary);
        return ResponseEntity.ok(createdRole);
    }

    /*
        Only SUPER_ADMIN can update role permissions
    */
    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody Set<String> permissionNames
    ) {
        Role role = roleService.assignPermissionsToRole(roleId, permissionNames);
        return ResponseEntity.ok(role);
    }

    /*
        Only SUPER_ADMIN can update role details such as description or base salary.
    */
    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> updateRole(
            @PathVariable Long roleId,
            @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(roleService.updateRole(roleId, request));
    }

    /*
        Only SUPER_ADMIN can delete non core custom roles.
    */
    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
    }

    /*
        Converts incoming JSON number/string values into BigDecimal safely.
        "baseSalary": 60000, "baseSalary": "60000"
        
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