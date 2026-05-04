package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.ReviewResponse;
import com.ByteKnights.com.resturarent_system.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/orders/{orderId}/reviews")
    public ResponseEntity<ApiResponse<String>> submitReview(
            @PathVariable Long orderId,
            @RequestBody ReviewSubmissionRequest request,
            Principal principal) {
        
        String userIdentifier = principal.getName();
        reviewService.submitReview(userIdentifier, orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", null));
    }

    @GetMapping("/reviews/recent")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getRecentReviews() {
        List<ReviewResponse> reviews = reviewService.getRecentReviews();
        return ResponseEntity.ok(ApiResponse.success("Recent reviews fetched successfully", reviews));
    }
}
