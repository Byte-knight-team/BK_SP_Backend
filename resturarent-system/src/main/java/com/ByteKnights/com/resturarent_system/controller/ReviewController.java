package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/reviews")
@CrossOrigin
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> submitReview(
            @PathVariable Long orderId,
            @RequestBody ReviewSubmissionRequest request,
            Principal principal) {
        
        String userIdentifier = principal.getName();
        reviewService.submitReview(userIdentifier, orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", null));
    }
}
