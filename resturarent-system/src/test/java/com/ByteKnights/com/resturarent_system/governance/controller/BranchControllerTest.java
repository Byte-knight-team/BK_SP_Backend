package com.ByteKnights.com.resturarent_system.governance.controller;

import com.ByteKnights.com.resturarent_system.controller.BranchController;
import com.ByteKnights.com.resturarent_system.dto.BranchResponse;
import com.ByteKnights.com.resturarent_system.dto.CreateBranchRequest;
import com.ByteKnights.com.resturarent_system.dto.UpdateBranchRequest;
import com.ByteKnights.com.resturarent_system.service.BranchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
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
 * Standalone controller-layer tests for BranchController.
 *
 * These tests check endpoint mapping, request JSON, response JSON,
 * and whether BranchController calls BranchService correctly.
 *
 * This does not load the full Spring Boot application context.
 * That avoids unrelated security filter/repository dependencies during controller testing.
 */
@ExtendWith(MockitoExtension.class)
class BranchControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private BranchService branchService;

    @BeforeEach
    void setUp() {
        BranchController branchController = new BranchController(branchService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(branchController)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void createBranch_shouldReturnCreatedBranchResponse() throws Exception {
        // Arrange
        CreateBranchRequest request = new CreateBranchRequest();
        request.setName("Main Branch");
        request.setAddress("123 Food Street");
        request.setContactNumber("0771234567");
        request.setEmail("main@cravehouse.com");

        BranchResponse response = buildBranchResponse(
                1L,
                "Main Branch",
                "123 Food Street",
                "0771234567",
                "main@cravehouse.com",
                "ACTIVE",
                "Branch created successfully"
        );

        when(branchService.createBranch(any(CreateBranchRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/admin/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Main Branch"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Branch created successfully"));

        verify(branchService, times(1)).createBranch(any(CreateBranchRequest.class));
    }

    @Test
    void getAllBranches_shouldReturnBranchList() throws Exception {
        // Arrange
        BranchResponse branchOne = buildBranchResponse(
                1L,
                "Main Branch",
                "123 Food Street",
                "0771234567",
                "main@cravehouse.com",
                "ACTIVE",
                null
        );

        BranchResponse branchTwo = buildBranchResponse(
                2L,
                "Second Branch",
                "456 Food Street",
                "0777654321",
                "second@cravehouse.com",
                "INACTIVE",
                null
        );

        when(branchService.getAllBranches()).thenReturn(List.of(branchOne, branchTwo));

        // Act + Assert
        mockMvc.perform(get("/api/admin/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Main Branch"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].name").value("Second Branch"))
                .andExpect(jsonPath("$[1].status").value("INACTIVE"));

        verify(branchService, times(1)).getAllBranches();
    }

    @Test
    void getBranchById_shouldReturnOneBranch() throws Exception {
        // Arrange
        BranchResponse response = buildBranchResponse(
                1L,
                "Main Branch",
                "123 Food Street",
                "0771234567",
                "main@cravehouse.com",
                "ACTIVE",
                null
        );

        when(branchService.getBranchById(1L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/api/admin/branches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Main Branch"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(branchService, times(1)).getBranchById(1L);
    }

    @Test
    void updateBranch_shouldReturnUpdatedBranchResponse() throws Exception {
        // Arrange
        UpdateBranchRequest request = new UpdateBranchRequest();
        request.setName("Updated Branch");
        request.setAddress("Updated Address");
        request.setContactNumber("0772222222");
        request.setEmail("updated@cravehouse.com");

        BranchResponse response = buildBranchResponse(
                1L,
                "Updated Branch",
                "Updated Address",
                "0772222222",
                "updated@cravehouse.com",
                "ACTIVE",
                "Branch updated successfully"
        );

        when(branchService.updateBranch(eq(1L), any(UpdateBranchRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(put("/api/admin/branches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Branch"))
                .andExpect(jsonPath("$.address").value("Updated Address"))
                .andExpect(jsonPath("$.message").value("Branch updated successfully"));

        verify(branchService, times(1)).updateBranch(eq(1L), any(UpdateBranchRequest.class));
    }

    @Test
    void activateBranch_shouldReturnActiveBranchResponse() throws Exception {
        // Arrange
        BranchResponse response = buildBranchResponse(
                1L,
                "Main Branch",
                "123 Food Street",
                "0771234567",
                "main@cravehouse.com",
                "ACTIVE",
                "Branch activated successfully"
        );

        when(branchService.activateBranch(1L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(patch("/api/admin/branches/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Branch activated successfully"));

        verify(branchService, times(1)).activateBranch(1L);
    }

    @Test
    void deactivateBranch_shouldReturnInactiveBranchResponse() throws Exception {
        // Arrange
        BranchResponse response = buildBranchResponse(
                1L,
                "Main Branch",
                "123 Food Street",
                "0771234567",
                "main@cravehouse.com",
                "INACTIVE",
                "Branch deactivated successfully"
        );

        when(branchService.deactivateBranch(1L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(patch("/api/admin/branches/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.message").value("Branch deactivated successfully"));

        verify(branchService, times(1)).deactivateBranch(1L);
    }

    /**
     * Helper method to avoid repeating BranchResponse builder code.
     */
    private BranchResponse buildBranchResponse(Long id,
                                               String name,
                                               String address,
                                               String contactNumber,
                                               String email,
                                               String status,
                                               String message) {
        return BranchResponse.builder()
                .id(id)
                .name(name)
                .address(address)
                .contactNumber(contactNumber)
                .email(email)
                .status(status)
                .createdAt(LocalDateTime.of(2026, 4, 27, 10, 0))
                .message(message)
                .build();
    }
}