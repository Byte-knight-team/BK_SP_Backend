package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    // Only SUPER_ADMIN can assign privileges to roles
    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody Set<String> permissionNames) {

        Role role = roleService.assignPermissionsToRole(roleId, permissionNames);
        return ResponseEntity.ok(role);
    }

    // SUPER_ADMIN and ADMIN can view privileges of a role
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Set<String>> getPermissions(@PathVariable Long roleId) {
        Set<String> perms = roleService.getPermissionsOfRole(roleId);
        return ResponseEntity.ok(perms);
    }

    // Only SUPER_ADMIN can create new roles
    @PostMapping("/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Role> createRole(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String desc = payload.get("description");
        Role role = roleService.createRole(name, desc);
        return ResponseEntity.ok(role);
    }
}