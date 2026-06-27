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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/*
 * Handles audit log retention.
 *
 * Main rule:
 * Old audit logs must be archived first.
 * Database rows are deleted only after archive storage succeeds.
 */
@Service
@RequiredArgsConstructor
public class AuditRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AuditRetentionService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditArchiveService auditArchiveService;
    private final AuditS3ArchiveService auditS3ArchiveService;

    /*
     * Enables or disables audit retention.
     */
    @Value("${audit.retention.enabled:true}")
    private boolean retentionEnabled;

    /*
     * Number of days to keep audit logs in the active database.
     *
     * Example:
     * 45 = around 1.5 months
     * 90 = around 3 months
     */
    @Value("${audit.retention.active-days:90}")
    private int activeDays;

    /*
     * Number of old logs processed in one scheduler run.
     */
    @Value("${audit.retention.batch-size:500}")
    private int batchSize;

    /*
     * Temporary local folder used before uploading to S3.
     */
    @Value("${audit.retention.archive-folder:audit-archives}")
    private String archiveFolder;

    /*
     * Storage mode.
     *
     * local = keep archive file locally
     * s3    = upload archive file to AWS S3
     */
    @Value("${audit.retention.storage:local}")
    private String archiveStorage;

    /*
     * Runs automatically using cron expression.
     *
     * Default:
     * Every Sunday at 2:00 AM.
     */
    @Scheduled(cron = "${audit.retention.cron:0 0 2 * * SUN}")
    public void runScheduledRetention() {
        runRetention();
    }

    /*
     * Main retention process.
     */
    public void runRetention() {
        if (!retentionEnabled) {
            log.info("Audit retention is disabled");
            return;
        }

        Path localArchiveFile = null;

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(activeDays);
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
             * Step 1:
             * Export old audit logs into a temporary local JSON file.
             */
            localArchiveFile = auditArchiveService.exportAuditLogsToJson(oldLogs, archiveFolder);

            /*
             * Step 2:
             * Upload archive to S3 if S3 storage is enabled.
             *
             * If this fails, an exception is thrown.
             * That means database delete will not happen.
             */
            String s3ObjectKey = null;

            if (isS3Storage()) {
                s3ObjectKey = auditS3ArchiveService.uploadAuditArchive(localArchiveFile);

                log.info(
                        "Audit archive uploaded to S3 successfully. s3ObjectKey={}",
                        s3ObjectKey
                );
            } else {
                log.info(
                        "Audit archive stored locally. archiveFile={}",
                        localArchiveFile
                );
            }

            /*
             * Step 3:
             * Delete old database rows only after archive storage succeeds.
             */
            List<Long> idsToDelete = oldLogs.stream()
                    .map(AuditLog::getId)
                    .toList();

            auditLogRepository.deleteAllByIdInBatch(idsToDelete);

            log.info(
                    "Audit retention completed. archivedCount={}, deletedCount={}, storage={}, archiveFile={}, s3ObjectKey={}",
                    oldLogs.size(),
                    idsToDelete.size(),
                    archiveStorage,
                    localArchiveFile,
                    s3ObjectKey
            );

            /*
             * Step 4:
             * If S3 is used, local file is temporary.
             * Delete it after successful S3 upload and DB delete.
             */
            if (isS3Storage()) {
                deleteTemporaryLocalFile(localArchiveFile);
            }

        } catch (Exception ex) {
            /*
             * Important:
             * Do not crash the backend if retention fails.
             * Also, database delete only happens after archive storage succeeds.
             */
            log.error(
                    "Audit retention failed. Old audit logs were not deleted unless archive storage succeeded.",
                    ex
            );
        }
    }

    private boolean isS3Storage() {
        return archiveStorage != null && archiveStorage.trim().equalsIgnoreCase("s3");
    }

    /*
     * Deletes temporary local archive file after successful S3 upload.
     */
    private void deleteTemporaryLocalFile(Path localArchiveFile) {
        if (localArchiveFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(localArchiveFile);
            log.info("Temporary local audit archive file deleted. file={}", localArchiveFile);
        } catch (Exception ex) {
            /*
             * Do not fail retention here.
             * Archive already exists in S3 and database cleanup already completed.
             */
            log.warn(
                    "Failed to delete temporary local audit archive file. file={}",
                    localArchiveFile,
                    ex
            );
        }
    }
}