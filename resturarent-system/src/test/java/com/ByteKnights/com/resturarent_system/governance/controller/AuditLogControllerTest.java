package com.ByteKnights.com.resturarent_system.governance.controller;

import com.ByteKnights.com.resturarent_system.controller.AuditLogController;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.AuditLogResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone controller-layer tests for AuditLogController.
 *
 * These tests check audit-log API endpoint mappings, query parameters,
 * pagination response JSON, single-log lookup, and whether the controller
 * calls AuditLogService correctly.
 *
 * This does not load the full Spring Boot application context.
 * That avoids unrelated security filter/database dependencies during controller testing.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        AuditLogController auditLogController = new AuditLogController(auditLogService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(auditLogController)
                .build();
    }

    @Test
    void getAuditLogs_shouldReturnPagedAuditLogsWithDefaultPagination() throws Exception {
        // Arrange
        AuditLogResponse response = AuditLogResponse.fromEntity(buildAuditLog(1L));

        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AuditLogResponse> page = new PageImpl<>(List.of(response), pageable, 1);

        when(auditLogService.getAuditLogs(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].module").value("AUTH"))
                .andExpect(jsonPath("$.content[0].eventType").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].severity").value("INFO"))
                .andExpect(jsonPath("$.content[0].targetType").value("AUTH"))
                .andExpect(jsonPath("$.content[0].description").value("Staff login successful"))
                .andExpect(jsonPath("$.content[0].actorUserId").value(10))
                .andExpect(jsonPath("$.content[0].actorEmail").value("admin@test.com"))
                .andExpect(jsonPath("$.content[0].actorRoleName").value("ADMIN"))
                .andExpect(jsonPath("$.content[0].branchId").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(auditLogService, times(1)).getAuditLogs(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                argThat(pageableArgument ->
                        pageableArgument.getPageNumber() == 0
                                && pageableArgument.getPageSize() == 20
                                && pageableArgument.getSort().getOrderFor("createdAt") != null
                                && pageableArgument.getSort().getOrderFor("createdAt").isDescending()
                )
        );
    }

    @Test
    void getAuditLogs_shouldPassFiltersAndPaginationToService() throws Exception {
        // Arrange
        AuditLogResponse response = AuditLogResponse.fromEntity(buildAuditLog(1L));

        PageRequest pageable = PageRequest.of(
                1,
                5,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AuditLogResponse> page = new PageImpl<>(List.of(response), pageable, 1);

        when(auditLogService.getAuditLogs(
                eq(AuditModule.AUTH),
                eq(AuditEventType.LOGIN_SUCCESS),
                eq(AuditStatus.SUCCESS),
                eq(2L),
                eq(10L),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 28)),
                any(Pageable.class)
        )).thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("module", "AUTH")
                        .param("eventType", "LOGIN_SUCCESS")
                        .param("status", "SUCCESS")
                        .param("branchId", "2")
                        .param("actorUserId", "10")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-28")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].module").value("AUTH"))
                .andExpect(jsonPath("$.content[0].eventType").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalElements").value(6));

        verify(auditLogService, times(1)).getAuditLogs(
                eq(AuditModule.AUTH),
                eq(AuditEventType.LOGIN_SUCCESS),
                eq(AuditStatus.SUCCESS),
                eq(2L),
                eq(10L),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 28)),
                argThat(pageableArgument ->
                        pageableArgument.getPageNumber() == 1
                                && pageableArgument.getPageSize() == 5
                                && pageableArgument.getSort().getOrderFor("createdAt") != null
                                && pageableArgument.getSort().getOrderFor("createdAt").isDescending()
                )
        );
    }

    @Test
    void getAuditLogById_shouldReturnOneAuditLog() throws Exception {
        // Arrange
        AuditLogResponse response = AuditLogResponse.fromEntity(buildAuditLog(1L));

        when(auditLogService.getAuditLogById(1L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/api/admin/audit-logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.module").value("AUTH"))
                .andExpect(jsonPath("$.eventType").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.severity").value("INFO"))
                .andExpect(jsonPath("$.targetType").value("AUTH"))
                .andExpect(jsonPath("$.description").value("Staff login successful"))
                .andExpect(jsonPath("$.actorUserId").value(10))
                .andExpect(jsonPath("$.actorEmail").value("admin@test.com"))
                .andExpect(jsonPath("$.actorRoleName").value("ADMIN"))
                .andExpect(jsonPath("$.branchId").value(2))
                .andExpect(jsonPath("$.targetId").value(10))
                .andExpect(jsonPath("$.httpMethod").value("POST"))
                .andExpect(jsonPath("$.endpoint").value("/api/auth/staff/login"));

        verify(auditLogService, times(1)).getAuditLogById(1L);
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
}