package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReviewImagePresignResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReviewImageResponse;

import java.util.List;

public interface ReviewImageStorageService {

    List<ReviewImagePresignResponse> createPresignedUploadUrls(String userIdentifier,
            List<ReviewSubmissionRequest.ReviewImageUploadRequest> files);

    String createPresignedDownloadUrl(String objectKey);

    ReviewImageResponse toResponse(String objectKey, String fileName, String contentType);
}