package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.entity.AuditLog;
import com.ByteKnights.com.resturarent_system.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/*
 * Handles audit retention.
 *
 * Rule:
 * Keep recent audit logs in the database.
 * Archive logs older than configured months.
 * Delete old database logs only after archive export succeeds.
 */
@Service
@RequiredArgsConstructor
public class AuditRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AuditRetentionService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditArchiveService auditArchiveService;

    /*
     * Whether the retention job is enabled.
     */
    @Value("${audit.retention.enabled:true}")
    private boolean retentionEnabled;

    /*
     * Number of months to keep in the active database.
     * Default: 3 months.
     */
    @Value("${audit.retention.active-months:3}")
    private int activeMonths;

    /*
     * Maximum number of old logs to process in one scheduler run.
     * This prevents the job from loading too much data at once.
     */
    @Value("${audit.retention.batch-size:500}")
    private int batchSize;

    /*
     * Folder where archive JSON files will be saved.
     */
    @Value("${audit.retention.archive-folder:audit-archives}")
    private String archiveFolder;

    /*
     * Runs automatically based on cron expression from application.properties.
     *
     * Default cron:
     * Every Sunday at 2:00 AM.
     */
    @Scheduled(cron = "${audit.retention.cron:0 0 2 * * SUN}")
    public void runScheduledRetention() {
        runRetention();
    }

    /*
     * Main retention process.
     * This method can also be called manually from tests.
     */
    public void runRetention() {
        if (!retentionEnabled) {
            log.info("Audit retention is disabled");
            return;
        }

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(activeMonths);

            int safeBatchSize = Math.max(1, batchSize);

            List<AuditLog> oldLogs = auditLogRepository.findByCreatedAtBeforeOrderByCreatedAtAsc(
                    cutoffDate,
                    PageRequest.of(0, safeBatchSize)
            );

            if (oldLogs.isEmpty()) {
                log.info("No old audit logs found for retention. cutoffDate={}", cutoffDate);
                return;
            }

            /*
             * Export first.
             * If this fails, the code jumps to catch block and nothing is deleted.
             */
            Path archiveFile = auditArchiveService.exportAuditLogsToJson(oldLogs, archiveFolder);

            /*
             * Delete only after archive file is successfully created.
             */
            List<Long> idsToDelete = oldLogs.stream()
                    .map(AuditLog::getId)
                    .toList();

            auditLogRepository.deleteAllByIdInBatch(idsToDelete);

            log.info(
                    "Audit retention completed. archivedCount={}, deletedCount={}, archiveFile={}",
                    oldLogs.size(),
                    idsToDelete.size(),
                    archiveFile
            );

        } catch (Exception ex) {
            /*
             * Important:
             * Do not throw the exception.
             * The scheduler should not crash the application.
             */
            log.error("Audit retention failed. No audit logs were deleted unless archive export succeeded.", ex);
        }
    }
}