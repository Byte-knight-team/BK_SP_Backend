package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;

public interface ReviewService {
    void submitReview(String userIdentifier, Long orderId, ReviewSubmissionRequest request);
}
