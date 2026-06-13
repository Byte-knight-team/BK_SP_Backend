package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReviewImagePresignResponse;
import com.ByteKnights.com.resturarent_system.dto.response.ReviewResponse;
import java.util.List;

public interface ReviewService {
    void submitReview(String userIdentifier, Long orderId, ReviewSubmissionRequest request);

    List<ReviewImagePresignResponse> createReviewImageUploadUrls(String userIdentifier,
            List<ReviewSubmissionRequest.ReviewImageUploadRequest> files);
    
    // Fetch recent order reviews for landing page
    List<ReviewResponse> getRecentReviews();
}
