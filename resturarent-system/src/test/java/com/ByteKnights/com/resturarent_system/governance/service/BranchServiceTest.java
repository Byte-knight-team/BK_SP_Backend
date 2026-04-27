package com.ByteKnights.com.resturarent_system.governance.service;

import com.ByteKnights.com.resturarent_system.dto.BranchResponse;
import com.ByteKnights.com.resturarent_system.dto.CreateBranchRequest;
import com.ByteKnights.com.resturarent_system.dto.UpdateBranchRequest;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchStatus;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.BranchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BranchService.
 *
 * These tests are for the governance / branch management module.
 * They use Mockito mocks, so they do not connect to the real MySQL database.
 */
@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BranchService branchService;

    @Test
    void createBranch_shouldCreateActiveBranch_whenRequestIsValid() {
        // Arrange: prepare a valid branch creation request
        CreateBranchRequest request = new CreateBranchRequest();
        request.setName("Main Branch");
        request.setAddress("123 Food Street");
        request.setContactNumber("0771234567");
        request.setEmail("main@cravehouse.com");

        when(branchRepository.existsByNameIgnoreCase("Main Branch")).thenReturn(false);

        // Mock save() so it returns the same branch with an ID like the database would do
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> {
            Branch branchToSave = invocation.getArgument(0);
            branchToSave.setId(1L);
            return branchToSave;
        });

        // Act: call the real service method
        BranchResponse response = branchService.createBranch(request);

        // Assert: check returned response
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Main Branch", response.getName());
        assertEquals("123 Food Street", response.getAddress());
        assertEquals("0771234567", response.getContactNumber());
        assertEquals("main@cravehouse.com", response.getEmail());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("Branch created successfully", response.getMessage());

        // Verify repository and audit service were called
        verify(branchRepository, times(1)).existsByNameIgnoreCase("Main Branch");
        verify(branchRepository, times(1)).save(any(Branch.class));

        verify(auditLogService, times(1)).logCurrentUserAction(
                eq(AuditModule.BRANCH),
                eq(AuditEventType.BRANCH_CREATED),
                eq(AuditStatus.SUCCESS),
                eq(AuditSeverity.INFO),
                eq(AuditTargetType.BRANCH),
                eq(1L),
                eq(1L),
                eq("Branch created successfully"),
                isNull(),
                anyMap()
        );
    }

    @Test
    void createBranch_shouldReturnValidationMessage_whenRequiredFieldsAreMissing() {
        // Arrange: request has missing required fields
        CreateBranchRequest request = new CreateBranchRequest();
        request.setName("");
        request.setAddress("");
        request.setContactNumber("");

        // Act
        BranchResponse response = branchService.createBranch(request);

        // Assert
        assertNotNull(response);
        assertNull(response.getId());
        assertTrue(response.getMessage().contains("Branch name is required"));
        assertTrue(response.getMessage().contains("Address is required"));
        assertTrue(response.getMessage().contains("Contact number is required"));

        // Save and audit should not happen when validation fails
        verify(branchRepository, never()).save(any(Branch.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void createBranch_shouldReturnDuplicateMessage_whenBranchNameAlreadyExists() {
        // Arrange
        CreateBranchRequest request = new CreateBranchRequest();
        request.setName("Main Branch");
        request.setAddress("123 Food Street");
        request.setContactNumber("0771234567");
        request.setEmail("main@cravehouse.com");

        when(branchRepository.existsByNameIgnoreCase("Main Branch")).thenReturn(true);

        // Act
        BranchResponse response = branchService.createBranch(request);

        // Assert
        assertNotNull(response);
        assertNull(response.getId());
        assertEquals("Branch name already exists", response.getMessage());

        // Save and audit should not happen for duplicate branch names
        verify(branchRepository, times(1)).existsByNameIgnoreCase("Main Branch");
        verify(branchRepository, never()).save(any(Branch.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void getBranchById_shouldReturnBranch_whenBranchExists() {
        // Arrange
        Branch branch = Branch.builder()
                .id(1L)
                .name("Main Branch")
                .address("123 Food Street")
                .contactNumber("0771234567")
                .email("main@cravehouse.com")
                .status(BranchStatus.ACTIVE)
                .build();

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));

        // Act
        BranchResponse response = branchService.getBranchById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Main Branch", response.getName());
        assertEquals("ACTIVE", response.getStatus());

        verify(branchRepository, times(1)).findById(1L);
    }

    @Test
    void getBranchById_shouldThrowException_whenBranchDoesNotExist() {
        // Arrange
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> branchService.getBranchById(99L)
        );

        assertEquals("Branch not found", exception.getMessage());
        verify(branchRepository, times(1)).findById(99L);
    }

    @Test
    void updateBranch_shouldUpdateBranch_whenRequestIsValid() {
        // Arrange
        Branch existingBranch = Branch.builder()
                .id(1L)
                .name("Old Branch")
                .address("Old Address")
                .contactNumber("0771111111")
                .email("old@cravehouse.com")
                .status(BranchStatus.ACTIVE)
                .build();

        UpdateBranchRequest request = new UpdateBranchRequest();
        request.setName("Updated Branch");
        request.setAddress("Updated Address");
        request.setContactNumber("0772222222");
        request.setEmail("updated@cravehouse.com");

        when(branchRepository.findById(1L)).thenReturn(Optional.of(existingBranch));
        when(branchRepository.existsByNameIgnoreCase("Updated Branch")).thenReturn(false);
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BranchResponse response = branchService.updateBranch(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Updated Branch", response.getName());
        assertEquals("Updated Address", response.getAddress());
        assertEquals("0772222222", response.getContactNumber());
        assertEquals("updated@cravehouse.com", response.getEmail());
        assertEquals("Branch updated successfully", response.getMessage());

        verify(branchRepository, times(1)).findById(1L);
        verify(branchRepository, times(1)).existsByNameIgnoreCase("Updated Branch");
        verify(branchRepository, times(1)).save(existingBranch);
        verify(auditLogService, times(1)).logCurrentUserAction(
                eq(AuditModule.BRANCH),
                eq(AuditEventType.BRANCH_UPDATED),
                eq(AuditStatus.SUCCESS),
                eq(AuditSeverity.INFO),
                eq(AuditTargetType.BRANCH),
                eq(1L),
                eq(1L),
                eq("Branch updated successfully"),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void deactivateBranch_shouldChangeStatusToInactive() {
        // Arrange
        Branch branch = Branch.builder()
                .id(1L)
                .name("Main Branch")
                .address("123 Food Street")
                .contactNumber("0771234567")
                .email("main@cravehouse.com")
                .status(BranchStatus.ACTIVE)
                .build();

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BranchResponse response = branchService.deactivateBranch(1L);

        // Assert
        assertNotNull(response);
        assertEquals("INACTIVE", response.getStatus());
        assertEquals("Branch deactivated successfully", response.getMessage());

        verify(branchRepository, times(1)).findById(1L);
        verify(branchRepository, times(1)).save(branch);
        verify(auditLogService, times(1)).logCurrentUserAction(
                eq(AuditModule.BRANCH),
                eq(AuditEventType.BRANCH_DEACTIVATED),
                eq(AuditStatus.SUCCESS),
                eq(AuditSeverity.INFO),
                eq(AuditTargetType.BRANCH),
                eq(1L),
                eq(1L),
                eq("Branch deactivated successfully"),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void activateBranch_shouldChangeStatusToActive() {
        // Arrange
        Branch branch = Branch.builder()
                .id(1L)
                .name("Main Branch")
                .address("123 Food Street")
                .contactNumber("0771234567")
                .email("main@cravehouse.com")
                .status(BranchStatus.INACTIVE)
                .build();

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BranchResponse response = branchService.activateBranch(1L);

        // Assert
        assertNotNull(response);
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("Branch activated successfully", response.getMessage());

        verify(branchRepository, times(1)).findById(1L);
        verify(branchRepository, times(1)).save(branch);
        verify(auditLogService, times(1)).logCurrentUserAction(
                eq(AuditModule.BRANCH),
                eq(AuditEventType.BRANCH_ACTIVATED),
                eq(AuditStatus.SUCCESS),
                eq(AuditSeverity.INFO),
                eq(AuditTargetType.BRANCH),
                eq(1L),
                eq(1L),
                eq("Branch activated successfully"),
                anyMap(),
                anyMap()
        );
    }
}