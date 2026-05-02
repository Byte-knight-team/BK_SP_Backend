package com.ByteKnights.com.resturarent_system.governance.service;

import com.ByteKnights.com.resturarent_system.dto.response.superadmin.AuditLogResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.AuditLogRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogService.
 *
 * These tests cover audit-log saving and retrieval for the governance module.
 * Repositories are mocked, so these tests do not connect to the real database.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logCurrentUserAction_shouldSaveAuditLogForAuthenticatedUser() throws Exception {
        // Arrange
        Role adminRole = buildRole(1L, "ADMIN");
        User adminUser = buildUser(10L, "admin@test.com", adminRole);
        Branch branch = buildBranch(2L, "Branch 02");
        Staff staff = buildStaff(5L, adminUser, branch);

        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("name", "Old Branch");

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("name", "New Branch");

        setAuthenticatedUser(adminUser);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(staffRepository.findByUserId(10L)).thenReturn(Optional.of(staff));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");

        // Act
        auditLogService.logCurrentUserAction(
                AuditModule.BRANCH,
                AuditEventType.BRANCH_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.BRANCH,
                2L,
                null,
                "Branch updated successfully",
                oldValues,
                newValues
        );

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();

        assertEquals(AuditModule.BRANCH, saved.getModule());
        assertEquals(AuditEventType.BRANCH_UPDATED, saved.getEventType());
        assertEquals(AuditStatus.SUCCESS, saved.getStatus());
        assertEquals(AuditSeverity.INFO, saved.getSeverity());
        assertEquals(AuditTargetType.BRANCH, saved.getTargetType());
        assertEquals("Branch updated successfully", saved.getDescription());

        assertEquals(10L, saved.getActorUserId());
        assertEquals("admin@test.com", saved.getActorEmail());
        assertEquals("ADMIN", saved.getActorRoleName());

        /*
         * branchId was null in the method call, so service should resolve it
         * from the authenticated user's Staff profile.
         */
        assertEquals(2L, saved.getBranchId());
        assertEquals(2L, saved.getTargetId());

        assertEquals("{\"mock\":\"json\"}", saved.getOldValuesJson());
        assertEquals("{\"mock\":\"json\"}", saved.getNewValuesJson());
    }

    @Test
    void logActionAsUser_shouldSaveAuditLogForExplicitActor() throws Exception {
        // Arrange
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("login", "success");

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"login\":\"success\"}");

        // Act
        auditLogService.logActionAsUser(
                10L,
                "admin@test.com",
                "ADMIN",
                2L,
                AuditModule.AUTH,
                AuditEventType.LOGIN_SUCCESS,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.AUTH,
                10L,
                "Staff login successful",
                null,
                newValues
        );

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();

        assertEquals(AuditModule.AUTH, saved.getModule());
        assertEquals(AuditEventType.LOGIN_SUCCESS, saved.getEventType());
        assertEquals(AuditStatus.SUCCESS, saved.getStatus());
        assertEquals(AuditSeverity.INFO, saved.getSeverity());
        assertEquals(AuditTargetType.AUTH, saved.getTargetType());

        assertEquals(10L, saved.getActorUserId());
        assertEquals("admin@test.com", saved.getActorEmail());
        assertEquals("ADMIN", saved.getActorRoleName());
        assertEquals(2L, saved.getBranchId());
        assertEquals(10L, saved.getTargetId());
        assertEquals("Staff login successful", saved.getDescription());

        assertNull(saved.getOldValuesJson());
        assertEquals("{\"login\":\"success\"}", saved.getNewValuesJson());
    }

    @Test
    void logAnonymousAction_shouldSaveAuditLogWithoutActorUserId() throws Exception {
        // Arrange
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("reason", "invalid email");

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"reason\":\"invalid email\"}");

        // Act
        auditLogService.logAnonymousAction(
                "missing@test.com",
                AuditModule.AUTH,
                AuditEventType.LOGIN_FAILED,
                AuditStatus.FAILURE,
                AuditSeverity.WARN,
                AuditTargetType.AUTH,
                null,
                "Staff login failed: invalid email",
                null,
                newValues
        );

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();

        assertEquals(AuditModule.AUTH, saved.getModule());
        assertEquals(AuditEventType.LOGIN_FAILED, saved.getEventType());
        assertEquals(AuditStatus.FAILURE, saved.getStatus());
        assertEquals(AuditSeverity.WARN, saved.getSeverity());
        assertEquals(AuditTargetType.AUTH, saved.getTargetType());

        assertNull(saved.getActorUserId());
        assertEquals("missing@test.com", saved.getActorEmail());
        assertNull(saved.getActorRoleName());
        assertNull(saved.getBranchId());
        assertNull(saved.getTargetId());
        assertEquals("Staff login failed: invalid email", saved.getDescription());
        assertEquals("{\"reason\":\"invalid email\"}", saved.getNewValuesJson());
    }

    @Test
    void logActionAsUser_shouldSanitizeSensitiveValuesBeforeSavingJson() throws Exception {
        // Arrange
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("temporaryPassword", "Temp@123");
        newValues.put("token", "jwt-token");
        newValues.put("safeField", "visible-value");

        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            Object sanitizedValue = invocation.getArgument(0);
            String text = sanitizedValue.toString();

            assertTrue(text.contains("temporaryPassword=***"));
            assertTrue(text.contains("token=***"));
            assertTrue(text.contains("safeField=visible-value"));

            return "{\"temporaryPassword\":\"***\",\"token\":\"***\",\"safeField\":\"visible-value\"}";
        });

        // Act
        auditLogService.logActionAsUser(
                10L,
                "admin@test.com",
                "ADMIN",
                2L,
                AuditModule.STAFF,
                AuditEventType.STAFF_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                20L,
                "Staff created successfully",
                null,
                newValues
        );

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();

        assertEquals("{\"temporaryPassword\":\"***\",\"token\":\"***\",\"safeField\":\"visible-value\"}",
                saved.getNewValuesJson());
    }

    @Test
    void getAuditLogs_shouldReturnPagedAuditLogResponses() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        AuditLog auditLog = buildAuditLog(1L);
        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);

        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<AuditLogResponse> response = auditLogService.getAuditLogs(
                AuditModule.AUTH,
                AuditEventType.LOGIN_SUCCESS,
                AuditStatus.SUCCESS,
                2L,
                10L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 28),
                pageable
        );

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());

        AuditLogResponse first = response.getContent().get(0);
        assertEquals(1L, first.getId());

        verify(auditLogRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAuditLogById_shouldReturnAuditLogResponse_whenLogExists() {
        // Arrange
        AuditLog auditLog = buildAuditLog(1L);

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(auditLog));

        // Act
        AuditLogResponse response = auditLogService.getAuditLogById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());

        verify(auditLogRepository, times(1)).findById(1L);
    }

    @Test
    void getAuditLogById_shouldThrowException_whenLogDoesNotExist() {
        // Arrange
        when(auditLogRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> auditLogService.getAuditLogById(99L)
        );

        assertEquals("Audit log not found", exception.getMessage());

        verify(auditLogRepository, times(1)).findById(99L);
    }

    private void setAuthenticatedUser(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private AuditLog buildAuditLog(Long id) {
        return AuditLog.builder()
                .id(id)
                .module(AuditModule.AUTH)
                .eventType(AuditEventType.LOGIN_SUCCESS)
                .status(AuditStatus.SUCCESS)
                .severity(AuditSeverity.INFO)
                .targetType(AuditTargetType.AUTH)
                .description("Staff login successful")
                .actorUserId(10L)
                .actorEmail("admin@test.com")
                .actorRoleName("ADMIN")
                .branchId(2L)
                .targetId(10L)
                .httpMethod("POST")
                .endpoint("/api/auth/staff/login")
                .ipAddress("127.0.0.1")
                .userAgent("JUnit Test")
                .oldValuesJson(null)
                .newValuesJson("{\"login\":\"success\"}")
                .createdAt(LocalDateTime.of(2026, 4, 28, 10, 0))
                .build();
    }

    private Role buildRole(Long id, String name) {
        return Role.builder()
                .id(id)
                .name(name)
                .description(name + " role")
                .build();
    }

    private User buildUser(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .fullName("Test User")
                .username("testuser")
                .email(email)
                .phone("0771234567")
                .password("encoded-password")
                .role(role)
                .isActive(true)
                .passwordChanged(true)
                .inviteStatus(InviteStatus.SENT)
                .emailSent(true)
                .build();
    }

    private Branch buildBranch(Long id, String name) {
        return Branch.builder()
                .id(id)
                .name(name)
                .address("Test Address")
                .contactNumber("0779999999")
                .email("branch@test.com")
                .status(BranchStatus.ACTIVE)
                .build();
    }

    private Staff buildStaff(Long id, User user, Branch branch) {
        return Staff.builder()
                .id(id)
                .user(user)
                .branch(branch)
                .salary(BigDecimal.ZERO)
                .employmentStatus(EmploymentStatus.ACTIVE)
                .build();
    }
}