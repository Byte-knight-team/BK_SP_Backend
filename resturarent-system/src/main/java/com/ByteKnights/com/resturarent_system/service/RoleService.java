package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.RoleSummaryResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateRoleRequest;
import com.ByteKnights.com.resturarent_system.entity.Privilege;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.repository.PrivilegeRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final UserRepository userRepository;

    // Core roles should not be deleted.
    // Also safer not to rename them.
    private static final Set<String> CORE_ROLES = Set.of(
            "SUPER_ADMIN",
            "ADMIN",
            "MANAGER",
            "CHEF",
            "RECEPTIONIST",
            "DELIVERY",
            "CUSTOMER"
    );

    @Autowired
    public RoleService(RoleRepository roleRepository,
                       PrivilegeRepository privilegeRepository,
                       UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.userRepository = userRepository;
    }

    // Only SUPER_ADMIN can assign privileges to a role
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Role assignPermissionsToRole(Long roleId, Set<String> permissionNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();

        // Extra safety for top-level roles
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

        // Replaces the entire permission set with the submitted set
        role.setPermissions(privileges);
        return roleRepository.save(role);
    }

    // SUPER_ADMIN and ADMIN can view permissions of a role
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Set<String> getPermissionsOfRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Set<String> perms = new HashSet<>();
        role.getPermissions().forEach(p -> perms.add(p.getName()));
        return perms;
    }

    // Only SUPER_ADMIN can create roles
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Role createRole(String name, String description) {
        String normalizedName = normalizeRoleName(name);

        if (roleRepository.findByName(normalizedName).isPresent()) {
            throw new RuntimeException("Role already exists");
        }

        Role role = Role.builder()
                .name(normalizedName)
                .description(description == null ? null : description.trim())
                .build();

        return roleRepository.save(role);
    }

    // SUPER_ADMIN and ADMIN can view all role summaries
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<RoleSummaryResponse> getAllRoleSummaries() {
        List<Role> roles = roleRepository.findAll();

        return roles.stream()
                .map(this::mapToRoleSummary)
                .toList();
    }

    // SUPER_ADMIN and ADMIN can view one role summary
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public RoleSummaryResponse getRoleSummaryById(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        return mapToRoleSummary(role);
    }

    // Only SUPER_ADMIN can update a role
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Role updateRole(Long roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        String currentRoleName = role.getName();
        boolean isCoreRole = CORE_ROLES.contains(currentRoleName);

        // If name is provided, validate and update it
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String normalizedNewName = normalizeRoleName(request.getName());

            // Do not allow renaming core roles
            if (isCoreRole && !currentRoleName.equals(normalizedNewName)) {
                throw new RuntimeException("Core roles cannot be renamed");
            }

            // Prevent duplicate role names
            roleRepository.findByName(normalizedNewName).ifPresent(existingRole -> {
                if (!existingRole.getId().equals(roleId)) {
                    throw new RuntimeException("Role name already exists");
                }
            });

            role.setName(normalizedNewName);
        }

        // Description can be updated even for core roles
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription().trim());
        }

        return roleRepository.save(role);
    }

    // Only SUPER_ADMIN can delete a role
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // Never allow deleting core roles
        if (CORE_ROLES.contains(role.getName())) {
            throw new RuntimeException("Core roles cannot be deleted");
        }

        // Never allow deleting a role that is assigned to users
        if (userRepository.existsByRole(role)) {
            throw new RuntimeException("Cannot delete role because it is assigned to one or more users");
        }

        roleRepository.delete(role);
    }

    private RoleSummaryResponse mapToRoleSummary(Role role) {
        int permissionCount = role.getPermissions() != null ? role.getPermissions().size() : 0;
        long activeUserCount = userRepository.countByRoleAndIsActiveTrue(role);

        return RoleSummaryResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissionCount(permissionCount)
                .activeUserCount(activeUserCount)
                .build();
    }

    private String normalizeRoleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Role name is required");
        }
        return name.trim().toUpperCase();
    }
}