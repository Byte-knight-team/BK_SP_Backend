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

/*
 * AuditLogSpecification builds dynamic database filters
 * for searching audit logs.
 */
public class AuditLogSpecification {

    /*
     * Private constructor prevents creating objects of this utility class.
     */
    private AuditLogSpecification() {
    }

    /*
     * Creates a Specification based on optional filter values.
     * Only non-null filters are added to the query.
     */
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

            /*
             * Filter by audit module.
             * Example: AUTH, STAFF, BRANCH, CONFIG.
             */
            if (module != null) {
                predicates.add(cb.equal(root.get("module"), module));
            }

            /*
             * Filter by specific event type.
             * Example: LOGIN_SUCCESS, STAFF_CREATED.
             */
            if (eventType != null) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }

            /*
             * Filter by success or failure status.
             */
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            /*
             * Filter logs belonging to a specific branch.
             */
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branchId"), branchId));
            }

            /*
             * Filter logs created by a specific actor/user.
             */
            if (actorUserId != null) {
                predicates.add(cb.equal(root.get("actorUserId"), actorUserId));
            }

            /*
             * Filter logs from the start of the selected date.
             */
            if (from != null) {
                LocalDateTime fromDateTime = from.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
            }

            /*
             * Filter logs until the end of the selected date.
             */
            if (to != null) {
                LocalDateTime toDateTime = to.atTime(23, 59, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDateTime));
            }

            /*
             * Combine all filters using AND.
             */
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}