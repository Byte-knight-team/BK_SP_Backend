package com.ByteKnights.com.resturarent_system.governance.controller;

import com.ByteKnights.com.resturarent_system.controller.StaffController;
import com.ByteKnights.com.resturarent_system.dto.CreateStaffRequest;
import com.ByteKnights.com.resturarent_system.dto.CreateStaffResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateStaffRequest;
import com.ByteKnights.com.resturarent_system.entity.InviteStatus;
import com.ByteKnights.com.resturarent_system.service.StaffService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone controller-layer tests for StaffController.
 *
 * These tests check staff API endpoint mappings, request JSON,
 * response JSON, and whether StaffController calls StaffService correctly.
 *
 * This does not load the full Spring Boot application context.
 * That avoids unrelated security filter/database dependencies during controller testing.
 */
@ExtendWith(MockitoExtension.class)
class StaffControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private StaffService staffService;

    @BeforeEach
    void setUp() {
        StaffController staffController = new StaffController(staffService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(staffController)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void createStaff_shouldReturnCreatedStaffResponse() throws Exception {
        // Arrange
        CreateStaffRequest request = new CreateStaffRequest();
        request.setFullName("Test Manager");
        request.setUsername("manager01");
        request.setEmail("manager01@test.com");
        request.setPhone("0771234567");
        request.setRoleName("MANAGER");
        request.setBranchId(1L);
        request.setSalary(new BigDecimal("75000.00"));

        CreateStaffResponse response = buildCreateStaffResponse(
                10L,
                "Test Manager",
                "manager01",
                "manager01@test.com",
                "0771234567",
                "MANAGER",
                true,
                false,
                InviteStatus.SENT,
                true,
                null,
                1L,
                "Main Branch",
                "Email sent successfully"
        );

        when(staffService.createStaff(any(CreateStaffRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/admin/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.fullName").value("Test Manager"))
                .andExpect(jsonPath("$.username").value("manager01"))
                .andExpect(jsonPath("$.email").value("manager01@test.com"))
                .andExpect(jsonPath("$.roleName").value("MANAGER"))
                .andExpect(jsonPath("$.branchId").value(1))
                .andExpect(jsonPath("$.branchName").value("Main Branch"))
                .andExpect(jsonPath("$.inviteStatus").value("SENT"))
                .andExpect(jsonPath("$.emailSent").value(true))
                .andExpect(jsonPath("$.message").value("Email sent successfully"));

        verify(staffService, times(1)).createStaff(any(CreateStaffRequest.class));
    }

    @Test
    void resendInvite_shouldReturnInviteResponse() throws Exception {
        // Arrange
        CreateStaffResponse response = buildCreateStaffResponse(
                10L,
                "Test Manager",
                "manager01",
                "manager01@test.com",
                "0771234567",
                "MANAGER",
                true,
                false,
                InviteStatus.SENT,
                true,
                null,
                1L,
                "Main Branch",
                "Email resent successfully"
        );

        when(staffService.resendInvite(10L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/admin/staff/10/resend-invite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.inviteStatus").value("SENT"))
                .andExpect(jsonPath("$.emailSent").value(true))
                .andExpect(jsonPath("$.message").value("Email resent successfully"));

        verify(staffService, times(1)).resendInvite(10L);
    }

    @Test
    void getAllStaff_shouldReturnStaffList() throws Exception {
        // Arrange
        StaffResponse staffOne = buildStaffResponse(
                10L,
                "Test Manager",
                "manager01",
                "manager01@test.com",
                "0771234567",
                "MANAGER",
                true,
                1L,
                "Main Branch",
                new BigDecimal("75000.00")
        );

        StaffResponse staffTwo = buildStaffResponse(
                11L,
                "Test Chef",
                "chef01",
                "chef01@test.com",
                "0771111111",
                "CHEF",
                false,
                1L,
                "Main Branch",
                new BigDecimal("65000.00")
        );

        when(staffService.getAllStaff()).thenReturn(List.of(staffOne, staffTwo));

        // Act + Assert
        mockMvc.perform(get("/api/admin/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].roleName").value("MANAGER"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].roleName").value("CHEF"))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(staffService, times(1)).getAllStaff();
    }

    @Test
    void getStaffById_shouldReturnOneStaffMember() throws Exception {
        // Arrange
        StaffResponse response = buildStaffResponse(
                10L,
                "Test Manager",
                "manager01",
                "manager01@test.com",
                "0771234567",
                "MANAGER",
                true,
                1L,
                "Main Branch",
                new BigDecimal("75000.00")
        );

        when(staffService.getStaffById(10L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/api/admin/staff/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.fullName").value("Test Manager"))
                .andExpect(jsonPath("$.email").value("manager01@test.com"))
                .andExpect(jsonPath("$.roleName").value("MANAGER"))
                .andExpect(jsonPath("$.branchName").value("Main Branch"));

        verify(staffService, times(1)).getStaffById(10L);
    }

    @Test
    void updateStaff_shouldReturnUpdatedStaffResponse() throws Exception {
        // Arrange
        UpdateStaffRequest request = new UpdateStaffRequest();
        request.setFullName("Updated Manager");
        request.setEmail("updated.manager@test.com");
        request.setPhone("0772222222");
        request.setRoleName("MANAGER");
        request.setBranchId(1L);
        request.setSalary(new BigDecimal("80000.00"));

        StaffResponse response = buildStaffResponse(
                10L,
                "Updated Manager",
                "manager01",
                "updated.manager@test.com",
                "0772222222",
                "MANAGER",
                true,
                1L,
                "Main Branch",
                new BigDecimal("80000.00")
        );

        when(staffService.updateStaff(eq(10L), any(UpdateStaffRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(put("/api/admin/staff/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.fullName").value("Updated Manager"))
                .andExpect(jsonPath("$.email").value("updated.manager@test.com"))
                .andExpect(jsonPath("$.phone").value("0772222222"))
                .andExpect(jsonPath("$.salary").value(80000.00));

        verify(staffService, times(1)).updateStaff(eq(10L), any(UpdateStaffRequest.class));
    }

    @Test
    void activateStaff_shouldReturnActiveStaffResponse() throws Exception {
        // Arrange
        StaffResponse response = buildStaffResponse(
                10L,
                "Test Manager",
                "manager01",
                "manager01@test.com",
                "0771234567",
                "MANAGER",
                true,
                1L,
                "Main Branch",
                new BigDecimal("75000.00")
        );

        when(staffService.activateStaff(10L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(patch("/api/admin/staff/10/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.active").value(true));

        verify(staffService, times(1)).activateStaff(10L);
    }

    @Test
    void deactivateStaff_shouldReturnInactiveStaffResponse() throws Exception {
        // Arrange
        StaffResponse response = buildStaffResponse(
                10L,
                "Test Manager",
                "manager01",
                "manager01@test.com",
                "0771234567",
                "MANAGER",
                false,
                1L,
                "Main Branch",
                new BigDecimal("75000.00")
        );

        when(staffService.deactivateStaff(10L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(patch("/api/admin/staff/10/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.active").value(false));

        verify(staffService, times(1)).deactivateStaff(10L);
    }

    @Test
    void getStaffByBranch_shouldReturnBranchStaffList() throws Exception {
        // Arrange
        StaffResponse response = buildStaffResponse(
                10L,
                "Test Manager",
                "manager01",
                "manager01@test.com",
                "0771234567",
                "MANAGER",
                true,
                1L,
                "Main Branch",
                new BigDecimal("75000.00")
        );

        when(staffService.getStaffByBranch(1L)).thenReturn(List.of(response));

        // Act + Assert
        mockMvc.perform(get("/api/admin/staff/branch/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].branchId").value(1))
                .andExpect(jsonPath("$[0].branchName").value("Main Branch"));

        verify(staffService, times(1)).getStaffByBranch(1L);
    }

    @Test
    void getStaffByRole_shouldReturnRoleStaffList() throws Exception {
        // Arrange
        StaffResponse response = buildStaffResponse(
                10L,
                "Test Manager",
                "manager01",
                "manager01@test.com",
                "0771234567",
                "MANAGER",
                true,
                1L,
                "Main Branch",
                new BigDecimal("75000.00")
        );

        when(staffService.getStaffByRole("MANAGER")).thenReturn(List.of(response));

        // Act + Assert
        mockMvc.perform(get("/api/admin/staff/role/MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roleName").value("MANAGER"));

        verify(staffService, times(1)).getStaffByRole("MANAGER");
    }

    /**
     * Helper for create staff / resend invite response.
     */
    private CreateStaffResponse buildCreateStaffResponse(Long id,
                                                         String fullName,
                                                         String username,
                                                         String email,
                                                         String phone,
                                                         String roleName,
                                                         Boolean active,
                                                         Boolean passwordChanged,
                                                         InviteStatus inviteStatus,
                                                         Boolean emailSent,
                                                         String temporaryPassword,
                                                         Long branchId,
                                                         String branchName,
                                                         String message) {
        return CreateStaffResponse.builder()
                .id(id)
                .fullName(fullName)
                .username(username)
                .email(email)
                .phone(phone)
                .roleName(roleName)
                .active(active)
                .passwordChanged(passwordChanged)
                .inviteStatus(inviteStatus)
                .emailSent(emailSent)
                .temporaryPassword(temporaryPassword)
                .branchId(branchId)
                .branchName(branchName)
                .message(message)
                .build();
    }

    /**
     * Helper for normal staff response.
     */
    private StaffResponse buildStaffResponse(Long id,
                                             String fullName,
                                             String username,
                                             String email,
                                             String phone,
                                             String roleName,
                                             Boolean active,
                                             Long branchId,
                                             String branchName,
                                             BigDecimal salary) {
        return StaffResponse.builder()
                .id(id)
                .userId(id)
                .fullName(fullName)
                .username(username)
                .email(email)
                .phone(phone)
                .roleName(roleName)
                .active(active)
                .passwordChanged(false)
                .inviteStatus(InviteStatus.SENT)
                .emailSent(true)
                .branchId(branchId)
                .branchName(branchName)
                .employmentStatus("ACTIVE")
                .salary(salary)
                .build();
    }
}