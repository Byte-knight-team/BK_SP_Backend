package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.entity.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Handles exporting old audit logs into archive files.
 *
 * This service only writes archive files.
 * It does not delete database records.
 */
@Service
@RequiredArgsConstructor
public class AuditArchiveService {

    private final ObjectMapper objectMapper;

    /*
     * Exports audit logs into a JSON archive file.
     *
     * Returns the archive file path only after the file is successfully created.
     */
    public Path exportAuditLogsToJson(List<AuditLog> logs, String archiveFolder) {
        try {
            if (logs == null || logs.isEmpty()) {
                throw new RuntimeException("No audit logs available for archive export");
            }

            Path archiveDirectory = Path.of(archiveFolder);
            Files.createDirectories(archiveDirectory);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

            String fileName = "audit-archive-" + timestamp + ".json";

            Path finalFile = archiveDirectory.resolve(fileName);
            Path tempFile = archiveDirectory.resolve(fileName + ".tmp");

            /*
             * Convert entities into simple map objects.
             * This avoids exposing JPA internals and keeps the archive clean.
             */
            List<Map<String, Object>> archiveData = logs.stream()
                    .map(this::toArchiveMap)
                    .toList();

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(tempFile.toFile(), archiveData);

            /*
             * Move temp file to final file only after writing succeeds.
             */
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);

            /*
             * Final safety check.
             */
            if (!Files.exists(finalFile) || Files.size(finalFile) == 0) {
                throw new RuntimeException("Audit archive file was not created correctly");
            }

            return finalFile;

        } catch (Exception ex) {
            throw new RuntimeException("Failed to export audit logs to archive file: " + ex.getMessage(), ex);
        }
    }

    /*
     * Creates a clean archive structure for one audit log.
     */
    private Map<String, Object> toArchiveMap(AuditLog log) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("id", log.getId());
        data.put("module", log.getModule() != null ? log.getModule().name() : null);
        data.put("eventType", log.getEventType() != null ? log.getEventType().name() : null);
        data.put("status", log.getStatus() != null ? log.getStatus().name() : null);
        data.put("severity", log.getSeverity() != null ? log.getSeverity().name() : null);
        data.put("targetType", log.getTargetType() != null ? log.getTargetType().name() : null);
        data.put("description", log.getDescription());

        data.put("actorUserId", log.getActorUserId());
        data.put("actorEmail", log.getActorEmail());
        data.put("actorRoleName", log.getActorRoleName());

        data.put("branchId", log.getBranchId());
        data.put("targetId", log.getTargetId());

        data.put("httpMethod", log.getHttpMethod());
        data.put("endpoint", log.getEndpoint());
        data.put("ipAddress", log.getIpAddress());
        data.put("userAgent", log.getUserAgent());

        data.put("oldValuesJson", log.getOldValuesJson());
        data.put("newValuesJson", log.getNewValuesJson());

        data.put("createdAt", log.getCreatedAt());

        return data;
    }
}