package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
                @Index(name = "idx_audit_logs_module", columnList = "module"),
                @Index(name = "idx_audit_logs_event_type", columnList = "event_type"),
                @Index(name = "idx_audit_logs_status", columnList = "status"),
                @Index(name = "idx_audit_logs_branch_id", columnList = "branch_id"),
                @Index(name = "idx_audit_logs_actor_user_id", columnList = "actor_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditModule module;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private AuditTargetType targetType;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "actor_role_name", length = 100)
    private String actorRoleName;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "http_method", length = 20)
    private String httpMethod;

    @Column(length = 500)
    private String endpoint;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "LONGTEXT")
    private String userAgent;

    @Column(name = "old_values_json", columnDefinition = "LONGTEXT")
    private String oldValuesJson;

    @Column(name = "new_values_json", columnDefinition = "LONGTEXT")
    private String newValuesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //setting current timestamp before saving
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}