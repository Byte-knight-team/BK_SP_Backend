package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
import com.ByteKnights.com.resturarent_system.dto.RoleSummaryResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateRoleRequest;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Privilege;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.repository.PrivilegeRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /*
     * Core roles are protected system roles.
     * They cannot be deleted, and their names cannot be changed.
     */
    private static final Set<String> CORE_ROLES = Set.of(
            "SUPER_ADMIN",
            "ADMIN",
            "MANAGER",
            "CHEF",
            "RECEPTIONIST",
            "DELIVERY",
            "CUSTOMER");

    /*
     * Roles that ADMIN should not receive from GET /api/admin/roles.
     * ADMIN only needs lower branch-level roles for staff create/edit.
     */
    private static final Set<String> ADMIN_HIDDEN_ROLE_NAMES = Set.of(
            "CUSTOMER",
            "SUPER_ADMIN",
            "ADMIN");

    @Autowired
    public RoleService(RoleRepository roleRepository,
            PrivilegeRepository privilegeRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /*
     * Updates the permissions assigned to a role..
     */
    public Role assignPermissionsToRole(Long roleId, Set<String> permissionNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();

        //SUPER_ADMIN can modify all roles
        if ((role.getName().equals("SUPER_ADMIN") || role.getName().equals("ADMIN"))
                && !principal.getUser().getRole().getName().equals("SUPER_ADMIN")) {
            throw new RuntimeException("Only Super Admin can modify Admin/Super Admin roles");
        }

        //prepare old values for audit log
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("roleId", role.getId());
        oldValues.put("roleName", role.getName());
        oldValues.put("permissionNames", extractPermissionNames(role.getPermissions()));

        //add privileges to role
        Set<Privilege> privileges = new HashSet<>();

        //check if privilege exists and add to role
        //Frontend send array of permission names so we need to check if privilege exists and
        //add to a set
        for (String permName : permissionNames) {
            Privilege privilege = privilegeRepository.findByName(permName)
                    //if privilege not found throw exception
                    .orElseThrow(() -> new RuntimeException("Privilege not found: " + permName));
            privileges.add(privilege);
        }

        //This replaces the old permissions with the new set
        role.setPermissions(privileges);
        Role savedRole = roleRepository.save(role);

        //prepare new values for audit log
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("roleId", savedRole.getId());
        newValues.put("roleName", savedRole.getName());
        newValues.put("permissionNames", extractPermissionNames(savedRole.getPermissions()));

        auditLogService.logCurrentUserAction(
                AuditModule.RBAC,
                AuditEventType.ROLE_PERMISSIONS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ROLE,
                savedRole.getId(),
                null,
                "Role permissions updated successfully",
                oldValues,
                newValues);

        return savedRole;
    }

    //get permissions of a role
    public Set<String> getPermissionsOfRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        //This creates an empty Set.
        Set<String> perms = new HashSet<>();
        //Convert Privilege objects into names
        role.getPermissions().forEach(p -> perms.add(p.getName()));

        //Return permission names
        return perms;
    }

    /*
     * Creates a new custom role.
     */
    @Auditable(module = AuditModule.RBAC, eventType = AuditEventType.ROLE_CREATED, targetType = AuditTargetType.ROLE, description = "Role created successfully", captureResultAsNewValue = false)
    public Role createRole(String name, String description, BigDecimal baseSalary) {
        String normalizedName = normalizeRoleName(name);

        //check if role already exists
        if (roleRepository.findByName(normalizedName).isPresent()) {
            throw new RuntimeException("Role already exists");
        }

        Role role = Role.builder()
                .name(normalizedName)
                .description(description == null ? null : description.trim())
                .baseSalary(normalizeSalary(baseSalary)) // Default salary for future staff with this role.
                .build();

        return roleRepository.save(role);
    }

    /*
     * Returns all roles
     */
    public List<RoleSummaryResponse> getAllRoleSummaries() {
        String currentUserRole = getCurrentAuthenticatedRoleName();

        //get all roles
        return roleRepository.findAll()
                .stream()
                .filter(role -> {
                    String roleName = normalizeRoleNameForAccessCheck(role.getName());

                    // ADMIN cannot access SUPER_ADMIN role
                    if ("ADMIN".equals(currentUserRole)) {
                        // ADMIN_HIDDEN_ROLE_NAMES = ["SUPER_ADMIN"]
                        return !ADMIN_HIDDEN_ROLE_NAMES.contains(roleName);
                    }

                    return true;
                })
                //sort by name
                .sorted((role1, role2) -> role1.getName().compareToIgnoreCase(role2.getName()))
                //convert to role summary
                .map(this::mapToRoleSummary)
                .toList();
    }

    // gets role details by ID
    public RoleSummaryResponse getRoleSummaryById(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        String currentUserRole = getCurrentAuthenticatedRoleName();
        String requestedRoleName = normalizeRoleNameForAccessCheck(role.getName());

        //ADMIN cannot access SUPER_ADMIN role
        if ("ADMIN".equals(currentUserRole) && ADMIN_HIDDEN_ROLE_NAMES.contains(requestedRoleName)) {
            throw new AccessDeniedException("ADMIN cannot access this role");
        }

        return mapToRoleSummary(role);
    }

    /*
     * Updates role details.
     */
    public Role updateRole(Long roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Map<String, Object> oldValues = buildRoleAuditSnapshot(role);

        String currentRoleName = role.getName();
        boolean isCoreRole = CORE_ROLES.contains(currentRoleName);

        /*
         * Update role name only if a new value is provided.
         * Core roles can be edited, but they cannot be renamed.
         */
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String normalizedNewName = normalizeRoleName(request.getName());

            if (isCoreRole && !currentRoleName.equals(normalizedNewName)) {
                throw new RuntimeException("Core roles cannot be renamed");
            }

            roleRepository.findByName(normalizedNewName).ifPresent(existingRole -> {
                if (!existingRole.getId().equals(roleId)) {
                    throw new RuntimeException("Role name already exists");
                }
            });

            role.setName(normalizedNewName);
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription().trim());
        }

        /*
         * Update the default salary for this role.
         * This does not automatically update existing staff salaries.
         */
        if (request.getBaseSalary() != null) {
            role.setBaseSalary(normalizeSalary(request.getBaseSalary()));
        }

        Role savedRole = roleRepository.save(role);

        auditLogService.logCurrentUserAction(
                AuditModule.RBAC,
                AuditEventType.ROLE_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ROLE,
                savedRole.getId(),
                null,
                "Role updated successfully",
                oldValues,
                buildRoleAuditSnapshot(savedRole));

        return savedRole;
    }

    /*
     * Deletes a custom role only if it is not a core role and is not assigned to any users.
     */
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        //Core roles cannot be deleted
        if (CORE_ROLES.contains(role.getName())) {
            throw new RuntimeException("Core roles cannot be deleted");
        }

        //if there are active users for a role, role cannot be deleted
        if (userRepository.existsByRole(role)) {
            throw new RuntimeException("Cannot delete role because it is assigned to one or more users");
        }

        Map<String, Object> oldValues = buildRoleAuditSnapshot(role);

        roleRepository.delete(role);

        auditLogService.logCurrentUserAction(
                AuditModule.RBAC,
                AuditEventType.ROLE_DELETED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ROLE,
                roleId,
                null,
                "Role deleted successfully",
                oldValues,
                null);
    }

    /*
     * Reads the current logged in user role from Spring Security authorities.
     */
    private String getCurrentAuthenticatedRoleName() {
        //Gets the current logged in authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getAuthorities() == null) {
            return "";
        }

        return authentication.getAuthorities()
                .stream()
                //Converts each authority object into text.
                .map(GrantedAuthority::getAuthority)
                //Keeps only values that start with ROLE_.
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                //Removes ROLE_ prefix
                .map(authority -> authority.replace("ROLE_", ""))
                //Returns the first match
                .findFirst()
                .orElse("");
    }

    /*
     * Normalizes role names for safe comparison.
     * " admin " -> "ADMIN"
     */
    private String normalizeRoleNameForAccessCheck(String roleName) {
        if (roleName == null) {
            return "";
        }

        return roleName.trim().toUpperCase();
    }

    //Maps role to role summary
    private RoleSummaryResponse mapToRoleSummary(Role role) {
        int permissionCount = role.getPermissions() != null ? role.getPermissions().size() : 0;
        long activeUserCount = userRepository.countByRoleAndIsActiveTrue(role);

        return RoleSummaryResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissionCount(permissionCount)
                .activeUserCount(activeUserCount)
                .baseSalary(role.getBaseSalary())
                .build();
    }

    //Normalizes role name
    private String normalizeRoleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Role name is required");
        }

        return name.trim().toUpperCase();
    }


    //Snapshot is used by manual audit logs to store old/new role state.
    private Map<String, Object> buildRoleAuditSnapshot(Role role) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("roleId", role.getId());
        snapshot.put("roleName", role.getName());
        snapshot.put("description", role.getDescription());
        snapshot.put("baseSalary", role.getBaseSalary());
        snapshot.put("permissionNames", extractPermissionNames(role.getPermissions()));
        snapshot.put("activeUserCount", userRepository.countByRoleAndIsActiveTrue(role));
        return snapshot;
    }

    //Extracts permission names from privileges
    //Privilege(name = "VIEW_STAFF") to VIEW_STAFF
    private Set<String> extractPermissionNames(Set<Privilege> privileges) {

        //TreeSet automatically sorts the names alphabetically and removes duplicates.
        Set<String> permissionNames = new TreeSet<>();

        if (privileges != null) {
            for (Privilege privilege : privileges) {
                if (privilege != null && privilege.getName() != null) {
                    permissionNames.add(privilege.getName());
                }
            }
        }

        return permissionNames;
    }

    //Salary validation helper, null salary becomes 0.00, 
    //negative salary is rejected, 
    //and valid salary is stored with 2 decimal places.
    private BigDecimal normalizeSalary(BigDecimal salary) {
        if (salary == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (salary.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Base salary cannot be negative");
        }

        return salary.setScale(2, RoundingMode.HALF_UP);
    }
}