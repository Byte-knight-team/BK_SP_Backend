package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.RoleSummaryResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateRoleRequest;
import org.springframework.security.access.AccessDeniedException;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private static final Set<String> CORE_ROLES = Set.of(
            "SUPER_ADMIN",
            "ADMIN",
            "MANAGER",
            "CHEF",
            "RECEPTIONIST",
            "DELIVERY",
            "CUSTOMER");

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

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Role assignPermissionsToRole(Long roleId, Set<String> permissionNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();

        if ((role.getName().equals("SUPER_ADMIN") || role.getName().equals("ADMIN"))
                && !principal.getUser().getRole().getName().equals("SUPER_ADMIN")) {
            throw new RuntimeException("Only Super Admin can modify Admin/Super Admin roles");
        }

        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("roleId", role.getId());
        oldValues.put("roleName", role.getName());
        oldValues.put("permissionNames", extractPermissionNames(role.getPermissions()));

        Set<Privilege> privileges = new HashSet<>();
        for (String permName : permissionNames) {
            Privilege privilege = privilegeRepository.findByName(permName)
                    .orElseThrow(() -> new RuntimeException("Privilege not found: " + permName));
            privileges.add(privilege);
        }

        role.setPermissions(privileges);
        Role savedRole = roleRepository.save(role);

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

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Set<String> getPermissionsOfRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Set<String> perms = new HashSet<>();
        role.getPermissions().forEach(p -> perms.add(p.getName()));
        return perms;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Role createRole(String name, String description, BigDecimal baseSalary) {
        String normalizedName = normalizeRoleName(name);

        if (roleRepository.findByName(normalizedName).isPresent()) {
            throw new RuntimeException("Role already exists");
        }

        Role role = Role.builder()
                .name(normalizedName)
                .description(description == null ? null : description.trim())

                /*
                 * New roles can have a default salary.
                 * If no salary is provided, it becomes 0.00.
                 */
                .baseSalary(normalizeSalary(baseSalary))
                .build();

        Role savedRole = roleRepository.save(role);

        auditLogService.logCurrentUserAction(
                AuditModule.RBAC,
                AuditEventType.ROLE_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ROLE,
                savedRole.getId(),
                null,
                "Role created successfully",
                null,
                buildRoleAuditSnapshot(savedRole));

        return savedRole;
    }

    /*
     * Returns role summaries.
     * 
     * SUPER_ADMIN:
     * - Can see all roles because SUPER_ADMIN manages RBAC.
     * 
     * ADMIN:
     * - Can only see branch-level assignable roles.
     * - ADMIN should not receive CUSTOMER, SUPER_ADMIN, or ADMIN.
     * - This is used by Create Staff / Edit Staff dropdown.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<RoleSummaryResponse> getAllRoleSummaries() {
        String currentUserRole = getCurrentAuthenticatedRoleName();

        return roleRepository.findAll()
                .stream()
                .filter(role -> {
                    String roleName = normalizeRoleNameForAccessCheck(role.getName());

                    /*
                     * ADMIN can only see lower branch-level roles.
                     * This keeps SUPER_ADMIN, ADMIN, and CUSTOMER hidden from ADMIN response.
                     */
                    if ("ADMIN".equals(currentUserRole)) {
                        return !ADMIN_HIDDEN_ROLE_NAMES.contains(roleName);
                    }

                    /*
                     * SUPER_ADMIN can see all roles.
                     */
                    return true;
                })
                .sorted((role1, role2) -> role1.getName().compareToIgnoreCase(role2.getName()))
                .map(this::mapToRoleSummary)
                .toList();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public RoleSummaryResponse getRoleSummaryById(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        /*
         * ADMIN cannot access SUPER_ADMIN, ADMIN, CUSTOMER
         */
        String currentUserRole = getCurrentAuthenticatedRoleName();
        String requestedRoleName = normalizeRoleNameForAccessCheck(role.getName());

        if ("ADMIN".equals(currentUserRole) && ADMIN_HIDDEN_ROLE_NAMES.contains(requestedRoleName)) {
            throw new AccessDeniedException("ADMIN cannot access this role");
        }

        return mapToRoleSummary(role);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Role updateRole(Long roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Map<String, Object> oldValues = buildRoleAuditSnapshot(role);

        String currentRoleName = role.getName();
        boolean isCoreRole = CORE_ROLES.contains(currentRoleName);

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
         * 
         * Important:
         * This does not automatically update existing staff salaries.
         * Existing staff keep their current Staff.salary unless edited manually.
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

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (CORE_ROLES.contains(role.getName())) {
            throw new RuntimeException("Core roles cannot be deleted");
        }

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
     * Roles that ADMIN should not receive from GET /api/admin/roles.
     * 
     * ADMIN only needs lower branch-level roles for staff create/edit.
     */
    private static final Set<String> ADMIN_HIDDEN_ROLE_NAMES = Set.of(
            "CUSTOMER",
            "SUPER_ADMIN",
            "ADMIN");

    /*
     * Reads the current logged-in user's role from Spring Security.
     * 
     * We use authorities because @PreAuthorize already works from authorities like:
     * ROLE_SUPER_ADMIN
     * ROLE_ADMIN
     */
    private String getCurrentAuthenticatedRoleName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getAuthorities() == null) {
            return "";
        }

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.replace("ROLE_", ""))
                .findFirst()
                .orElse("");
    }

    /*
     * Normalizes role names for safe comparison.
     * 
     * Example:
     * " admin " -> "ADMIN"
     */
    private String normalizeRoleNameForAccessCheck(String roleName) {
        if (roleName == null) {
            return "";
        }

        return roleName.trim().toUpperCase();
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

                /*
                 * Return salary to frontend so Roles page can show/edit it.
                 */
                .baseSalary(role.getBaseSalary())
                .build();
    }

    private String normalizeRoleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Role name is required");
        }
        return name.trim().toUpperCase();
    }

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

    private Set<String> extractPermissionNames(Set<Privilege> privileges) {
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

    /*
     * Salary validation helper.
     * 
     * Rules:
     * - Null salary becomes 0.00.
     * - Negative salary is not allowed.
     * - Salary is stored with 2 decimal places.
     */
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