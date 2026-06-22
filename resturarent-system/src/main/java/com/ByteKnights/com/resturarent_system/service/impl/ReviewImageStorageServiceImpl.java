package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.config.AwsS3Properties;
import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReviewImagePresignResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReviewImageResponse;
import com.ByteKnights.com.resturarent_system.exception.CheckoutException;
import com.ByteKnights.com.resturarent_system.service.ReviewImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles all AWS S3 interactions for review images.
 *
 * Upload flow:
 *   1. Frontend calls createPresignedUploadUrls() to get signed PUT URLs.
 *   2. Frontend uploads binary files directly to S3 using those URLs.
 *   3. Frontend then submits the review with the returned object keys.
 *
 * Download flow:
 *   When reviews are fetched, createPresignedDownloadUrl() generates a
 *   time-limited GET URL so the browser can display images from a private bucket.
 */
@Service
public class ReviewImageStorageServiceImpl implements ReviewImageStorageService {

    // Presigned URLs expire after 15 minutes — enough time for the upload flow to complete
    private static final long DEFAULT_PRESIGN_EXPIRATION_MINUTES = 15L;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private final AwsS3Properties properties;
    private final S3Presigner s3Presigner;

    public ReviewImageStorageServiceImpl(AwsS3Properties properties, S3Presigner s3Presigner) {
        this.properties = properties;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Generates a presigned PUT URL for each requested file.
     * The URL allows the browser to upload directly to S3 without going through the backend.
     */
    @Override
    public List<ReviewImagePresignResponse> createPresignedUploadUrls(String userIdentifier,
            List<ReviewSubmissionRequest.ReviewImageUploadRequest> files) {
        List<ReviewImagePresignResponse> responses = new ArrayList<>();
        if (files == null) {
            return responses;
        }

        for (ReviewSubmissionRequest.ReviewImageUploadRequest file : files) {
            validateUploadRequest(file);

            // Build a unique S3 key scoped under the user's prefix: reviews/{user}/{uuid}.{ext}
            String objectKey = buildObjectKey(userIdentifier, file.getFileName());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(builder -> builder
                    .signatureDuration(getExpirationDuration())
                    .putObjectRequest(putObjectRequest));

            responses.add(ReviewImagePresignResponse.builder()
                    .fileName(file.getFileName())
                    .contentType(file.getContentType())
                    .objectKey(objectKey)
                    .uploadUrl(presignedRequest.url().toString())
                    .expiresInSeconds(getExpirationDuration().toSeconds())
                    .build());
        }

        return responses;
    }

    /**
     * Generates a presigned GET URL for reading an image from a private S3 bucket.
     * Used when building the review response for display on the landing page.
     */
    @Override
    public String createPresignedDownloadUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(builder -> builder
                .signatureDuration(getExpirationDuration())
                .getObjectRequest(getObjectRequest));

        return presignedRequest.url().toString();
    }

    /** Converts stored image metadata into a response DTO with a fresh presigned download URL. */
    @Override
    public ReviewImageResponse toResponse(String objectKey, String fileName, String contentType) {
        return ReviewImageResponse.builder()
                .imageKey(objectKey)
                .imageUrl(createPresignedDownloadUrl(objectKey))
                .fileName(fileName)
                .contentType(contentType)
                .build();
    }

    private Duration getExpirationDuration() {
        return Duration.ofMinutes(DEFAULT_PRESIGN_EXPIRATION_MINUTES);
    }

    private void validateUploadRequest(ReviewSubmissionRequest.ReviewImageUploadRequest file) {
        if (file == null) {
            throw new CheckoutException(HttpStatus.BAD_REQUEST, "Invalid image upload request.");
        }

        if (file.getFileName() == null || file.getFileName().trim().isEmpty()) {
            throw new CheckoutException(HttpStatus.BAD_REQUEST, "Image file name is required.");
        }

        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType().toLowerCase(Locale.ROOT))) {
            throw new CheckoutException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, and WEBP review images are allowed.");
        }
    }

    /**
     * Produces the S3 object key for a new upload.
     * Pattern: reviews/{sanitized-user}/{uuid}.{ext}
     * UUID guarantees uniqueness; user prefix allows per-user key validation later.
     */
    private String buildObjectKey(String userIdentifier, String fileName) {
        String prefix = sanitizePrefix(userIdentifier);
        String extension = extractExtension(fileName);
        return String.format("reviews/%s/%s%s", prefix, UUID.randomUUID(), extension);
    }

    // Converts the user identifier into a safe path segment (only a-z, 0-9, ., _, -)
    private String sanitizePrefix(String value) {
        if (value == null || value.isBlank()) {
            return "anonymous";
        }

        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-");
    }

    // Extracts the file extension and guards against path-traversal via very long extensions
    private String extractExtension(String fileName) {
        String normalized = fileName.trim();
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return "";
        }

        String extension = normalized.substring(dotIndex).toLowerCase(Locale.ROOT);
        return extension.length() <= 10 ? extension : "";
    }
}