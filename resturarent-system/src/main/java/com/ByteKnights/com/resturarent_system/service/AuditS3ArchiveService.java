package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.config.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * Uploads audit archive JSON files to AWS S3.
 *
 * Important:
 * This service only uploads files.
 * It does not delete audit logs from the database.
 */
@Service
@RequiredArgsConstructor
public class AuditS3ArchiveService {

    private final S3Client s3Client;
    private final AwsS3Properties awsS3Properties;

    @Value("${audit.retention.s3-prefix:audit-archives}")
    private String s3Prefix;

    /*
     * Uploads the archive file to S3 and returns the S3 object key.
     *
     * If this method throws an exception, the retention service must NOT delete
     * old audit logs from the database.
     */
    public String uploadAuditArchive(Path archiveFile) {
        if (archiveFile == null) {
            throw new RuntimeException("Archive file path is missing");
        }

        String bucketName = awsS3Properties.getBucketName();

        if (bucketName == null || bucketName.isBlank()) {
            throw new RuntimeException("AWS S3 bucket name is not configured");
        }

        String objectKey = buildS3ObjectKey(archiveFile);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("application/json")
                .build();

        s3Client.putObject(request, RequestBody.fromFile(archiveFile));

        return objectKey;
    }

    /*
     * Builds a clean S3 path like:
     *
     * audit-archives/2026/06/audit-archive-20260627-143000.json
     */
    private String buildS3ObjectKey(Path archiveFile) {
        LocalDateTime now = LocalDateTime.now();

        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));

        String cleanPrefix = cleanPrefix(s3Prefix);

        return cleanPrefix + "/"
                + year + "/"
                + month + "/"
                + archiveFile.getFileName().toString();
    }

    /*
     * Removes starting/ending slashes from the prefix.
     *
     * Example:
     * /audit-archives/ -> audit-archives
     */
    private String cleanPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "audit-archives";
        }

        String cleaned = prefix.trim();

        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        return cleaned.isBlank() ? "audit-archives" : cleaned;
    }
}