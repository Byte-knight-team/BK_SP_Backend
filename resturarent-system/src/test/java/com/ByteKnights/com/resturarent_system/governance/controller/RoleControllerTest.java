package com.ByteKnights.com.resturarent_system.governance.controller;

import com.ByteKnights.com.resturarent_system.controller.RoleController;
import com.ByteKnights.com.resturarent_system.dto.RoleSummaryResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateRoleRequest;
import com.ByteKnights.com.resturarent_system.entity.Privilege;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone controller-layer tests for RoleController.
 *
 * These tests check RBAC API endpoint mappings, request JSON,
 * response JSON, and whether RoleController calls RoleService correctly.
 *
 * This does not load the full Spring Boot application context.
 * That avoids unrelated security filter/database dependencies during controller testing.
 */
@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        RoleController roleController = new RoleController(roleService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(roleController)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllRoles_shouldReturnRoleSummaryList() throws Exception {
        // Arrange
        RoleSummaryResponse adminRole = buildRoleSummary(
                1L,
                "ADMIN",
                "Branch admin",
                5,
                2L,
                BigDecimal.ZERO
        );

        RoleSummaryResponse chefRole = buildRoleSummary(
                2L,
                "CHEF",
                "Kitchen staff",
                3,
                4L,
                new BigDecimal("65000.00")
        );

        when(roleService.getAllRoleSummaries()).thenReturn(List.of(adminRole, chefRole));

        // Act + Assert
        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("ADMIN"))
                .andExpect(jsonPath("$[0].permissionCount").value(5))
                .andExpect(jsonPath("$[0].activeUserCount").value(2))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("CHEF"))
                .andExpect(jsonPath("$[1].permissionCount").value(3))
                .andExpect(jsonPath("$[1].activeUserCount").value(4));

        verify(roleService, times(1)).getAllRoleSummaries();
    }

    @Test
    void getRoleSummary_shouldReturnOneRoleSummary() throws Exception {
        // Arrange
        RoleSummaryResponse response = buildRoleSummary(
                2L,
                "CHEF",
                "Kitchen staff",
                3,
                4L,
                new BigDecimal("65000.00")
        );

        when(roleService.getRoleSummaryById(2L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/api/admin/roles/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("CHEF"))
                .andExpect(jsonPath("$.description").value("Kitchen staff"))
                .andExpect(jsonPath("$.permissionCount").value(3))
                .andExpect(jsonPath("$.activeUserCount").value(4));

        verify(roleService, times(1)).getRoleSummaryById(2L);
    }

    @Test
    void getPermissions_shouldReturnPermissionNameSet() throws Exception {
        // Arrange
        Set<String> permissions = Set.of("CREATE_STAFF", "VIEW_STAFF");

        when(roleService.getPermissionsOfRole(2L)).thenReturn(permissions);

        // Act + Assert
        mockMvc.perform(get("/api/admin/roles/2/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(roleService, times(1)).getPermissionsOfRole(2L);
    }

    @Test
    void createRole_shouldReturnCreatedRole() throws Exception {
        // Arrange
        Map<String, Object> payload = Map.of(
                "name", "cashier",
                "description", "Handles cashier operations",
                "baseSalary", "55000"
        );

        Role createdRole = buildRole(
                10L,
                "CASHIER",
                "Handles cashier operations",
                new BigDecimal("55000.00")
        );

        when(roleService.createRole(
                eq("cashier"),
                eq("Handles cashier operations"),
                eq(new BigDecimal("55000"))
        )).thenReturn(createdRole);

        // Act + Assert
        mockMvc.perform(post("/api/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("CASHIER"))
                .andExpect(jsonPath("$.description").value("Handles cashier operations"));

        verify(roleService, times(1)).createRole(
                eq("cashier"),
                eq("Handles cashier operations"),
                eq(new BigDecimal("55000"))
        );
    }

    @Test
    void assignPermissions_shouldReturnUpdatedRole() throws Exception {
        // Arrange
        Set<String> permissionNames = Set.of("CREATE_STAFF", "VIEW_STAFF");

        Privilege createStaff = buildPrivilege(1L, "CREATE_STAFF", "Create staff");
        Privilege viewStaff = buildPrivilege(2L, "VIEW_STAFF", "View staff");

        Role updatedRole = buildRole(
                2L,
                "ADMIN",
                "Branch admin",
                BigDecimal.ZERO
        );
        updatedRole.setPermissions(new HashSet<>(Set.of(createStaff, viewStaff)));

        when(roleService.assignPermissionsToRole(eq(2L), anySet())).thenReturn(updatedRole);

        // Act + Assert
        mockMvc.perform(put("/api/admin/roles/2/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permissionNames)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("ADMIN"))
                .andExpect(jsonPath("$.permissions.length()").value(2));

        verify(roleService, times(1)).assignPermissionsToRole(eq(2L), anySet());
    }

    @Test
    void updateRole_shouldReturnUpdatedRole() throws Exception {
        // Arrange
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("head_cashier");
        request.setDescription("Updated cashier role");
        request.setBaseSalary(new BigDecimal("65000.00"));

        Role updatedRole = buildRole(
                10L,
                "HEAD_CASHIER",
                "Updated cashier role",
                new BigDecimal("65000.00")
        );

        when(roleService.updateRole(eq(10L), any(UpdateRoleRequest.class))).thenReturn(updatedRole);

        // Act + Assert
        mockMvc.perform(put("/api/admin/roles/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("HEAD_CASHIER"))
                .andExpect(jsonPath("$.description").value("Updated cashier role"));

        verify(roleService, times(1)).updateRole(eq(10L), any(UpdateRoleRequest.class));
    }

    @Test
    void deleteRole_shouldReturnSuccessMessage() throws Exception {
        // Arrange
        doNothing().when(roleService).deleteRole(10L);

        // Act + Assert
        mockMvc.perform(delete("/api/admin/roles/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role deleted successfully"));

        verify(roleService, times(1)).deleteRole(10L);
    }

    private RoleSummaryResponse buildRoleSummary(Long id,
                                                 String name,
                                                 String description,
                                                 Integer permissionCount,
                                                 Long activeUserCount,
                                                 BigDecimal baseSalary) {
        return RoleSummaryResponse.builder()
                .id(id)
                .name(name)
                .description(description)
                .permissionCount(permissionCount)
                .activeUserCount(activeUserCount)
                .baseSalary(baseSalary)
                .build();
    }

    private Role buildRole(Long id,
                           String name,
                           String description,
                           BigDecimal baseSalary) {
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