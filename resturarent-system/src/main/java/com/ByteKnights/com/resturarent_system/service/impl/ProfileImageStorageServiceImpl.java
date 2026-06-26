package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.config.AwsS3Properties;
import com.ByteKnights.com.resturarent_system.exception.CheckoutException;
import com.ByteKnights.com.resturarent_system.service.ProfileImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProfileImageStorageServiceImpl implements ProfileImageStorageService {

    private static final long DEFAULT_PRESIGN_EXPIRATION_MINUTES = 15L;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private final AwsS3Properties properties;
    private final S3Presigner s3Presigner;

    public ProfileImageStorageServiceImpl(AwsS3Properties properties, S3Presigner s3Presigner) {
        this.properties = properties;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public String createPresignedUploadUrl(String objectKey, String contentType) {
        validateContentType(contentType);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(builder -> builder
                .signatureDuration(Duration.ofMinutes(DEFAULT_PRESIGN_EXPIRATION_MINUTES))
                .putObjectRequest(putObjectRequest));

        return presignedRequest.url().toString();
    }

    @Override
    public String createPresignedDownloadUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(builder -> builder
                .signatureDuration(Duration.ofMinutes(DEFAULT_PRESIGN_EXPIRATION_MINUTES))
                .getObjectRequest(getObjectRequest));

        return presignedRequest.url().toString();
    }

    @Override
    public String buildObjectKey(String userIdentifier, String fileName) {
        String prefix = sanitizePrefix(userIdentifier);
        String extension = extractExtension(fileName);
        return String.format("profiles/%s/%s%s", prefix, UUID.randomUUID(), extension);
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new CheckoutException(HttpStatus.BAD_REQUEST, "Unsupported image format. Allowed: JPG, PNG, WEBP");
        }
    }

    private String sanitizePrefix(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
            return "anonymous";
        }
        return userIdentifier.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            return "";
        }
        String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        // Clean up any weird characters in the extension just in case
        ext = ext.replaceAll("[^.a-z0-9]", "");
        return ext.length() > 5 ? "" : ext; // e.g. .jpeg is 5 chars
    }
}
