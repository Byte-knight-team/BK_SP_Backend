package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /*
     * Used by audit retention.
     * Finds old audit logs before the cutoff date.
     *
     * Example:
     * If cutoffDate = today minus 3 months,
     * this returns logs older than 3 months.
     */
    List<AuditLog> findByCreatedAtBeforeOrderByCreatedAtAsc(
            LocalDateTime cutoffDate,
            Pageable pageable
    );
}