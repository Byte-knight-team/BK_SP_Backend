package com.ByteKnights.com.resturarent_system.dto.response.superadmin;

import com.ByteKnights.com.resturarent_system.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private String module;
    private String eventType;
    private String status;
    private String severity;
    private String targetType;
    private String description;

    private Long actorUserId;
    private String actorEmail;
    private String actorRoleName;

    private Long branchId;
    private Long targetId;

    private String httpMethod;
    private String endpoint;
    private String ipAddress;
    private String userAgent;

    private String oldValuesJson;
    private String newValuesJson;

    private LocalDateTime createdAt;

    public static AuditLogResponse fromEntity(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .module(log.getModule() != null ? log.getModule().name() : null)
                .eventType(log.getEventType() != null ? log.getEventType().name() : null)
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .severity(log.getSeverity() != null ? log.getSeverity().name() : null)
                .targetType(log.getTargetType() != null ? log.getTargetType().name() : null)
                .description(log.getDescription())
                .actorUserId(log.getActorUserId())
                .actorEmail(log.getActorEmail())
                .actorRoleName(log.getActorRoleName())
                .branchId(log.getBranchId())
                .targetId(log.getTargetId())
                .httpMethod(log.getHttpMethod())
                .endpoint(log.getEndpoint())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .oldValuesJson(log.getOldValuesJson())
                .newValuesJson(log.getNewValuesJson())
                .createdAt(log.getCreatedAt())
                .build();
    }
}