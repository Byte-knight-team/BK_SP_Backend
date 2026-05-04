package com.ByteKnights.com.resturarent_system.governance.service;

import com.ByteKnights.com.resturarent_system.dto.RoleSummaryResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateRoleRequest;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.PrivilegeRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.RoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoleService.
 *
 * These tests cover the governance / RBAC role-management logic.
 * Repositories and audit logging are mocked, so these tests do not use the real database.
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PrivilegeRepository privilegeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RoleService roleService;

    /**
     * Clear Spring Security context after each test.
     * This prevents one test's authenticated role from affecting another test.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRole_shouldCreateCustomRole_whenRoleDoesNotExist() {
        // Arrange
        when(roleRepository.findByName("CASHIER")).thenReturn(Optional.empty());

        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role roleToSave = invocation.getArgument(0);
            roleToSave.setId(10L);
            return roleToSave;
        });

        // Act
        Role result = roleService.createRole(
                " cashier ",
                " Handles cashier operations ",
                new BigDecimal("55000")
        );

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("CASHIER", result.getName());
        assertEquals("Handles cashier operations", result.getDescription());
        assertEquals(new BigDecimal("55000.00"), result.getBaseSalary());

        verify(roleRepository, times(1)).findByName("CASHIER");
        verify(roleRepository, times(1)).save(any(Role.class));

        verify(auditLogService, times(1)).logCurrentUserAction(
                eq(AuditModule.RBAC),
                eq(AuditEventType.ROLE_CREATED),
                eq(AuditStatus.SUCCESS),
                eq(AuditSeverity.INFO),
                eq(AuditTargetType.ROLE),
                eq(10L),
                isNull(),
                eq("Role created successfully"),
                isNull(),
                anyMap()
        );
    }

    @Test
    void createRole_shouldThrowException_whenRoleNameAlreadyExists() {
        // Arrange
        Role existingRole = buildRole(1L, "CASHIER", "Existing cashier role", BigDecimal.ZERO);

        when(roleRepository.findByName("CASHIER")).thenReturn(Optional.of(existingRole));

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> roleService.createRole("cashier", "Duplicate role", BigDecimal.ZERO)
        );

        assertEquals("Role already exists", exception.getMessage());

        verify(roleRepository, times(1)).findByName("CASHIER");
        verify(roleRepository, never()).save(any(Role.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void createRole_shouldThrowException_whenBaseSalaryIsNegative() {
        // Arrange
        when(roleRepository.findByName("CASHIER")).thenReturn(Optional.empty());

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> roleService.createRole("cashier", "Cashier role", new BigDecimal("-1000"))
        );

        assertEquals("Base salary cannot be negative", exception.getMessage());

        verify(roleRepository, times(1)).findByName("CASHIER");
        verify(roleRepository, never()).save(any(Role.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void getAllRoleSummaries_shouldReturnAllRoles_whenCurrentUserIsSuperAdmin() {
        // Arrange
        setAuthenticatedRole("SUPER_ADMIN");

        Role superAdminRole = buildRole(1L, "SUPER_ADMIN", "Global admin", BigDecimal.ZERO);
        Role adminRole = buildRole(2L, "ADMIN", "Branch admin", BigDecimal.ZERO);
        Role chefRole = buildRole(3L, "CHEF", "Kitchen staff", new BigDecimal("65000.00"));

        when(roleRepository.findAll()).thenReturn(List.of(chefRole, superAdminRole, adminRole));
        when(userRepository.countByRoleAndIsActiveTrue(any(Role.class))).thenReturn(0L);

        // Act
        List<RoleSummaryResponse> result = roleService.getAllRoleSummaries();

        // Assert
        assertEquals(3, result.size());

        // Sorted alphabetically by role name: ADMIN, CHEF, SUPER_ADMIN
        assertEquals("ADMIN", result.get(0).getName());
        assertEquals("CHEF", result.get(1).getName());
        assertEquals("SUPER_ADMIN", result.get(2).getName());

        verify(roleRepository, times(1)).findAll();
    }

    @Test
    void getAllRoleSummaries_shouldHideHighLevelRoles_whenCurrentUserIsAdmin() {
        // Arrange
        setAuthenticatedRole("ADMIN");

        Role superAdminRole = buildRole(1L, "SUPER_ADMIN", "Global admin", BigDecimal.ZERO);
        Role adminRole = buildRole(2L, "ADMIN", "Branch admin", BigDecimal.ZERO);
        Role customerRole = buildRole(3L, "CUSTOMER", "Customer role", BigDecimal.ZERO);
        Role chefRole = buildRole(4L, "CHEF", "Kitchen staff", new BigDecimal("65000.00"));
        Role receptionistRole = buildRole(5L, "RECEPTIONIST", "Front desk", new BigDecimal("60000.00"));

        when(roleRepository.findAll()).thenReturn(
                List.of(superAdminRole, adminRole, customerRole, chefRole, receptionistRole)
        );
        when(userRepository.countByRoleAndIsActiveTrue(any(Role.class))).thenReturn(0L);

        // Act
        List<RoleSummaryResponse> result = roleService.getAllRoleSummaries();

        // Assert
        assertEquals(2, result.size());
        assertEquals("CHEF", result.get(0).getName());
        assertEquals("RECEPTIONIST", result.get(1).getName());

        assertTrue(result.stream().noneMatch(role -> role.getName().equals("SUPER_ADMIN")));
        assertTrue(result.stream().noneMatch(role -> role.getName().equals("ADMIN")));
        assertTrue(result.stream().noneMatch(role -> role.getName().equals("CUSTOMER")));

        verify(roleRepository, times(1)).findAll();
    }

    @Test
    void getRoleSummaryById_shouldThrowAccessDenied_whenAdminAccessesHiddenRole() {
        // Arrange
        setAuthenticatedRole("ADMIN");

        Role superAdminRole = buildRole(1L, "SUPER_ADMIN", "Global admin", BigDecimal.ZERO);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(superAdminRole));

        // Act + Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> roleService.getRoleSummaryById(1L)
        );

        assertEquals("ADMIN cannot access this role", exception.getMessage());

        verify(roleRepository, times(1)).findById(1L);
    }

    @Test
    void getPermissionsOfRole_shouldReturnPermissionNames() {
        // Arrange
        Privilege createStaff = buildPrivilege(1L, "CREATE_STAFF", "Create staff");
        Privilege viewStaff = buildPrivilege(2L, "VIEW_STAFF", "View staff");

        Role adminRole = buildRole(2L, "ADMIN", "Branch admin", BigDecimal.ZERO);
        adminRole.setPermissions(new HashSet<>(Set.of(createStaff, viewStaff)));

        when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));

        // Act
        Set<String> result = roleService.getPermissionsOfRole(2L);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains("CREATE_STAFF"));
        assertTrue(result.contains("VIEW_STAFF"));

        verify(roleRepository, times(1)).findById(2L);
    }

    @Test
    void updateRole_shouldUpdateCustomRoleDetails() {
        // Arrange
        Role cashierRole = buildRole(10L, "CASHIER", "Old description", new BigDecimal("50000.00"));

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("head_cashier");
        request.setDescription("Updated cashier role");
        request.setBaseSalary(new BigDecimal("65000"));

        when(roleRepository.findById(10L)).thenReturn(Optional.of(cashierRole));
        when(roleRepository.findByName("HEAD_CASHIER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.countByRoleAndIsActiveTrue(any(Role.class))).thenReturn(0L);

        // Act
        Role result = roleService.updateRole(10L, request);

        // Assert
        assertNotNull(result);
        assertEquals("HEAD_CASHIER", result.getName());
        assertEquals("Updated cashier role", result.getDescription());
        assertEquals(new BigDecimal("65000.00"), result.getBaseSalary());

        verify(roleRepository, times(1)).findById(10L);
        verify(roleRepository, times(1)).findByName("HEAD_CASHIER");
        verify(roleRepository, times(1)).save(cashierRole);

        verify(auditLogService, times(1)).logCurrentUserAction(
                eq(AuditModule.RBAC),
                eq(AuditEventType.ROLE_UPDATED),
                eq(AuditStatus.SUCCESS),
                eq(AuditSeverity.INFO),
                eq(AuditTargetType.ROLE),
                eq(10L),
                isNull(),
                eq("Role updated successfully"),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void updateRole_shouldThrowException_whenTryingToRenameCoreRole() {
        // Arrange
        Role adminRole = buildRole(2L, "ADMIN", "Branch admin", BigDecimal.ZERO);

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("BRANCH_ADMIN");

        when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
        when(userRepository.countByRoleAndIsActiveTrue(adminRole)).thenReturn(0L);

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> roleService.updateRole(2L, request)
        );

        assertEquals("Core roles cannot be renamed", exception.getMessage());

        verify(roleRepository, times(1)).findById(2L);
        verify(roleRepository, never()).save(any(Role.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void deleteRole_shouldThrowException_whenRoleIsCoreRole() {
        // Arrange
        Role chefRole = buildRole(3L, "CHEF", "Kitchen staff", new BigDecimal("65000.00"));

        when(roleRepository.findById(3L)).thenReturn(Optional.of(chefRole));

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> roleService.deleteRole(3L)
        );

        assertEquals("Core roles cannot be deleted", exception.getMessage());

        verify(roleRepository, times(1)).findById(3L);
        verify(roleRepository, never()).delete(any(Role.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void deleteRole_shouldThrowException_whenRoleIsAssignedToUsers() {
        // Arrange
        Role cashierRole = buildRole(10L, "CASHIER", "Cashier role", new BigDecimal("55000.00"));

        when(roleRepository.findById(10L)).thenReturn(Optional.of(cashierRole));
        when(userRepository.existsByRole(cashierRole)).thenReturn(true);

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> roleService.deleteRole(10L)
        );

        assertEquals("Cannot delete role because it is assigned to one or more users", exception.getMessage());

        verify(roleRepository, times(1)).findById(10L);
        verify(userRepository, times(1)).existsByRole(cashierRole);
        verify(roleRepository, never()).delete(any(Role.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void deleteRole_shouldDeleteCustomUnusedRole() {
        // Arrange
        Role cashierRole = buildRole(10L, "CASHIER", "Cashier role", new BigDecimal("55000.00"));

        when(roleRepository.findById(10L)).thenReturn(Optional.of(cashierRole));
        when(userRepository.existsByRole(cashierRole)).thenReturn(false);
        when(userRepository.countByRoleAndIsActiveTrue(cashierRole)).thenReturn(0L);

        // Act
        roleService.deleteRole(10L);

        // Assert
        verify(roleRepository, times(1)).findById(10L);
        verify(userRepository, times(1)).existsByRole(cashierRole);
        verify(roleRepository, times(1)).delete(cashierRole);

        verify(auditLogService, times(1)).logCurrentUserAction(
                eq(AuditModule.RBAC),
                eq(AuditEventType.ROLE_DELETED),
                eq(AuditStatus.SUCCESS),
                eq(AuditSeverity.INFO),
                eq(AuditTargetType.ROLE),
                eq(10L),
                isNull(),
                eq("Role deleted successfully"),
                anyMap(),
                isNull()
        );
    }

    /**
     * Creates a fake authenticated role in Spring Security.
     * RoleService reads ROLE_SUPER_ADMIN / ROLE_ADMIN from authorities.
     */
    private void setAuthenticatedRole(String roleName) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "test-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Role buildRole(Long id, String name, String description, BigDecimal baseSalary) {
        return Role.builder()
                .id(id)
                .name(name)
                .description(description)
                .baseSalary(baseSalary)
                .permissions(new HashSet<>())
                .build();
    }

    private Privilege buildPrivilege(Long id, String name, String description) {
        return Privilege.builder()
                .id(id)
                .name(name)
                .description(description)
                .build();
    }
}