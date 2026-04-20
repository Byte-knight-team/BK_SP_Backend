package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.entity.Privilege;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.repository.PrivilegeRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository,
                       PrivilegeRepository privilegeRepository) {
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
    }

    // Only SUPER_ADMIN can assign privileges to a role
    // This matches your current runtime RBAC model:
    // token -> role only, security checks -> role only
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Role assignPermissionsToRole(Long roleId, Set<String> permissionNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();

        // Extra protection:
        // only SUPER_ADMIN can modify ADMIN or SUPER_ADMIN roles
        if ((role.getName().equals("SUPER_ADMIN") || role.getName().equals("ADMIN"))
                && !principal.getUser().getRole().getName().equals("SUPER_ADMIN")) {
            throw new RuntimeException("Only Super Admin can modify Admin/Super Admin roles");
        }

        Set<Privilege> privileges = new HashSet<>();
        for (String permName : permissionNames) {
            Privilege privilege = privilegeRepository.findByName(permName)
                    .orElseThrow(() -> new RuntimeException("Privilege not found: " + permName));
            privileges.add(privilege);
        }

        role.setPermissions(privileges);
        return roleRepository.save(role);
    }

    // SUPER_ADMIN and ADMIN can view privileges of a role
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Set<String> getPermissionsOfRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Set<String> perms = new HashSet<>();
        role.getPermissions().forEach(p -> perms.add(p.getName()));
        return perms;
    }

    // Only SUPER_ADMIN can create new roles
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Role createRole(String name, String description) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new RuntimeException("Role already exists");
        }

        Role role = Role.builder()
                .name(name)
                .description(description)
                .build();

        return roleRepository.save(role);
    }
}