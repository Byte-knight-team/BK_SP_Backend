package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditLog;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> withFilters(
            AuditModule module,
            AuditEventType eventType,
            AuditStatus status,
            Long branchId,
            Long actorUserId,
            LocalDate from,
            LocalDate to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (module != null) {
                predicates.add(cb.equal(root.get("module"), module));
            }

            if (eventType != null) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (branchId != null) {
                predicates.add(cb.equal(root.get("branchId"), branchId));
            }

            if (actorUserId != null) {
                predicates.add(cb.equal(root.get("actorUserId"), actorUserId));
            }

            if (from != null) {
                LocalDateTime fromDateTime = from.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
            }

            if (to != null) {
                LocalDateTime toDateTime = to.atTime(23, 59, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDateTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}