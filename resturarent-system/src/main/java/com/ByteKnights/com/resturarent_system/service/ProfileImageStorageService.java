package com.ByteKnights.com.resturarent_system.service;

public interface ProfileImageStorageService {
    
    /**
     * Creates a presigned PUT URL for uploading a profile picture.
     * @param objectKey the exact S3 object key
     * @param contentType the MIME type of the file
     * @return the presigned upload URL
     */
    String createPresignedUploadUrl(String objectKey, String contentType);

    /**
     * Creates a presigned GET URL for downloading/viewing a profile picture.
     * @param objectKey the exact S3 object key
     * @return the presigned download URL
     */
    String createPresignedDownloadUrl(String objectKey);

    /**
     * Extracts the object key from the generated upload URL structure if needed,
     * or builds it using the same logic.
     */
    String buildObjectKey(String userIdentifier, String fileName);
}
