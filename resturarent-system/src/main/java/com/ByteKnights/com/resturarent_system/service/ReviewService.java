package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.ReviewResponse;
import java.util.List;

public interface ReviewService {
    void submitReview(String userIdentifier, Long orderId, ReviewSubmissionRequest request);
    
    // Fetch recent order reviews for landing page
    List<ReviewResponse> getRecentReviews();
}
