package com.ByteKnights.com.resturarent_system.governance.controller;

import com.ByteKnights.com.resturarent_system.auth.AuthService;
import com.ByteKnights.com.resturarent_system.controller.AuthController;
import com.ByteKnights.com.resturarent_system.dto.ChangePasswordRequest;
import com.ByteKnights.com.resturarent_system.dto.LoginResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffLoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone controller-layer tests for AuthController.
 *
 * These tests check authentication API endpoint mappings,
 * request JSON, response JSON, and whether AuthController calls AuthService correctly.
 *
 * This does not load the full Spring Boot application context.
 * That avoids unrelated security filter/database dependencies during controller testing.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void staffLogin_shouldReturnLoginResponse() throws Exception {
        // Arrange
        StaffLoginRequest request = new StaffLoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");

        LoginResponse response = LoginResponse.builder()
                .id(10L)
                .username("admin01")
                .email("admin@test.com")
                .roleName("ADMIN")
                .active(true)
                .passwordChanged(true)
                .branchId(2L)
                .branchName("Branch 02")
                .token("mock-jwt-token")
                .tokenType("Bearer")
                .build();

        when(authService.loginStaff(any(StaffLoginRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.username").value("admin01"))
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.roleName").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordChanged").value(true))
                .andExpect(jsonPath("$.branchId").value(2))
                .andExpect(jsonPath("$.branchName").value("Branch 02"))
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authService, times(1)).loginStaff(any(StaffLoginRequest.class));
    }

    @Test
    void changePassword_shouldReturnSuccessMessage() throws Exception {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");

        when(authService.changePassword(any(ChangePasswordRequest.class)))
                .thenReturn("Password changed successfully");

        // Act + Assert
        mockMvc.perform(put("/api/auth/staff/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));

        verify(authService, times(1)).changePassword(any(ChangePasswordRequest.class));
    }
}