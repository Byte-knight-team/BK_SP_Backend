package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.RoleSummaryResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateRoleRequest;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // Get all role summaries for roles page
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<RoleSummaryResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoleSummaries());
    }

    // Get one role summary
    @GetMapping("/{roleId}/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<RoleSummaryResponse> getRoleSummary(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getRoleSummaryById(roleId));
    }

    // Only SUPER_ADMIN can assign privileges to roles
    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody Set<String> permissionNames) {

        Role role = roleService.assignPermissionsToRole(roleId, permissionNames);
        return ResponseEntity.ok(role);
    }

    // SUPER_ADMIN and ADMIN can view role privileges
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Set<String>> getPermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getPermissionsOfRole(roleId));
    }

    // Only SUPER_ADMIN can create new roles
    @PostMapping("/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> createRole(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String desc = payload.get("description");
        return ResponseEntity.ok(roleService.createRole(name, desc));
    }

    // Only SUPER_ADMIN can update role name/description
    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> updateRole(@PathVariable Long roleId,
                                           @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(roleId, request));
    }

    // Only SUPER_ADMIN can delete roles
    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
    }
}