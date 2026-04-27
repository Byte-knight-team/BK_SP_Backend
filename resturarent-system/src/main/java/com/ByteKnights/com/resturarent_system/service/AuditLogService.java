package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.superadmin.AuditLogResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.AuditLogRepository;
import com.ByteKnights.com.resturarent_system.repository.AuditLogSpecification;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logCurrentUserAction(
            AuditModule module,
            AuditEventType eventType,
            AuditStatus status,
            AuditSeverity severity,
            AuditTargetType targetType,
            Long targetId,
            Long branchId,
            String description,
            Object oldValues,
            Object newValues
    ) {
        try {
            User actor = getCurrentAuthenticatedUser();
            Staff actorStaff = staffRepository.findByUserId(actor.getId()).orElse(null);

            Long resolvedBranchId = branchId;
            if (resolvedBranchId == null && actorStaff != null && actorStaff.getBranch() != null) {
                resolvedBranchId = actorStaff.getBranch().getId();
            }

            AuditLog auditLog = AuditLog.builder()
                    .module(module)
                    .eventType(eventType)
                    .status(status)
                    .severity(severity)
                    .targetType(targetType)
                    .description(description)
                    .actorUserId(actor.getId())
                    .actorEmail(actor.getEmail())
                    .actorRoleName(actor.getRole() != null ? actor.getRole().getName() : null)
                    .branchId(resolvedBranchId)
                    .targetId(targetId)
                    .httpMethod(getCurrentRequestMethod())
                    .endpoint(getCurrentRequestUri())
                    .ipAddress(getCurrentIpAddress())
                    .userAgent(getCurrentUserAgent())
                    .oldValuesJson(toJson(oldValues))
                    .newValuesJson(toJson(newValues))
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log for current user action", e);
        }
    }

    @Transactional
    public void logActionAsUser(
            Long actorUserId,
            String actorEmail,
            String actorRoleName,
            Long branchId,
            AuditModule module,
            AuditEventType eventType,
            AuditStatus status,
            AuditSeverity severity,
            AuditTargetType targetType,
            Long targetId,
            String description,
            Object oldValues,
            Object newValues
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .module(module)
                    .eventType(eventType)
                    .status(status)
                    .severity(severity)
                    .targetType(targetType)
                    .description(description)
                    .actorUserId(actorUserId)
                    .actorEmail(actorEmail)
                    .actorRoleName(actorRoleName)
                    .branchId(branchId)
                    .targetId(targetId)
                    .httpMethod(getCurrentRequestMethod())
                    .endpoint(getCurrentRequestUri())
                    .ipAddress(getCurrentIpAddress())
                    .userAgent(getCurrentUserAgent())
                    .oldValuesJson(toJson(oldValues))
                    .newValuesJson(toJson(newValues))
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log for explicit actor action", e);
        }
    }

    @Transactional
    public void logAnonymousAction(
            String actorEmail,
            AuditModule module,
            AuditEventType eventType,
            AuditStatus status,
            AuditSeverity severity,
            AuditTargetType targetType,
            Long targetId,
            String description,
            Object oldValues,
            Object newValues
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .module(module)
                    .eventType(eventType)
                    .status(status)
                    .severity(severity)
                    .targetType(targetType)
                    .description(description)
                    .actorUserId(null)
                    .actorEmail(actorEmail)
                    .actorRoleName(null)
                    .branchId(null)
                    .targetId(targetId)
                    .httpMethod(getCurrentRequestMethod())
                    .endpoint(getCurrentRequestUri())
                    .ipAddress(getCurrentIpAddress())
                    .userAgent(getCurrentUserAgent())
                    .oldValuesJson(toJson(oldValues))
                    .newValuesJson(toJson(newValues))
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save anonymous audit log", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            AuditModule module,
            AuditEventType eventType,
            AuditStatus status,
            Long branchId,
            Long actorUserId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        Specification<AuditLog> specification = AuditLogSpecification.withFilters(
                module,
                eventType,
                status,
                branchId,
                actorUserId,
                from,
                to
        );

        return auditLogRepository.findAll(specification, pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit log not found"));

        return AuditLogResponse.fromEntity(auditLog);
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new RuntimeException("Authenticated user not found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof JwtUserPrincipal jwtUser) {
            return userRepository.findByEmail(jwtUser.getEmail())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        if (principal instanceof User user) {
            return userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        String email = authentication.getName();
        if (email != null && !email.trim().isEmpty()) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        throw new RuntimeException("Authenticated user not found");
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            Object sanitized = sanitizeValue(value);
            return objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"Failed to serialize audit payload\"}";
        }
    }

    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                String key = String.valueOf(entry.getKey());

                if (isSensitiveKey(key)) {
                    sanitized.put(key, "***");
                } else {
                    sanitized.put(key, sanitizeValue(entry.getValue()));
                }
            }
            return sanitized;
        }

        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::sanitizeValue)
                    .collect(Collectors.toList());
        }

        return value;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }

        String normalized = key.trim().toLowerCase();
        return normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("temporarypassword");
    }

    private String getCurrentRequestMethod() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getMethod() : null;
    }

    private String getCurrentRequestUri() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getRequestURI() : null;
    }

    private String getCurrentIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String getCurrentUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return attributes != null ? attributes.getRequest() : null;
    }
}