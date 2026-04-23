package com.ByteKnights.com.resturarent_system.export.provider;

import com.ByteKnights.com.resturarent_system.entity.AuditLog;
import com.ByteKnights.com.resturarent_system.export.ExportFormat;
import com.ByteKnights.com.resturarent_system.export.ExportTarget;
import com.ByteKnights.com.resturarent_system.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuditLogExportProvider implements ExportDataProvider {

    private final AuditLogRepository auditLogRepository;

    @Override
    public ExportTarget getTarget() {
        return ExportTarget.AUDIT_LOGS;
    }

    @Override
    public String getBaseFileName() {
        return "audit-logs";
    }

    @Override
    public Set<ExportFormat> getSupportedFormats() {
        return EnumSet.of(ExportFormat.CSV, ExportFormat.JSON);
    }

    @Override
    public List<LinkedHashMap<String, Object>> getCsvRows() {
        return auditLogRepository.findAll(
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                ).stream()
                .map(this::toCsvRow)
                .collect(Collectors.toList());
    }

    @Override
    public Object getJsonData() {
        return auditLogRepository.findAll(
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
    }

    private LinkedHashMap<String, Object> toCsvRow(AuditLog auditLog) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();

        row.put("id", auditLog.getId());
        row.put("module", auditLog.getModule() != null ? auditLog.getModule().name() : null);
        row.put("eventType", auditLog.getEventType() != null ? auditLog.getEventType().name() : null);
        row.put("status", auditLog.getStatus() != null ? auditLog.getStatus().name() : null);
        row.put("severity", auditLog.getSeverity() != null ? auditLog.getSeverity().name() : null);
        row.put("targetType", auditLog.getTargetType() != null ? auditLog.getTargetType().name() : null);
        row.put("description", auditLog.getDescription());
        row.put("actorUserId", auditLog.getActorUserId());
        row.put("actorEmail", auditLog.getActorEmail());
        row.put("actorRoleName", auditLog.getActorRoleName());
        row.put("branchId", auditLog.getBranchId());
        row.put("targetId", auditLog.getTargetId());
        row.put("httpMethod", auditLog.getHttpMethod());
        row.put("endpoint", auditLog.getEndpoint());
        row.put("ipAddress", auditLog.getIpAddress());
        row.put("userAgent", auditLog.getUserAgent());
        row.put("oldValuesJson", auditLog.getOldValuesJson());
        row.put("newValuesJson", auditLog.getNewValuesJson());
        row.put("createdAt", auditLog.getCreatedAt());

        return row;
    }
}