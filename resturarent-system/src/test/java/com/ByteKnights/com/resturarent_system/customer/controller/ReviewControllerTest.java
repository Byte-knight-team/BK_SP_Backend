package com.ByteKnights.com.resturarent_system.customer.controller;

import com.ByteKnights.com.resturarent_system.controller.ReviewController;
import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.ReviewResponse;
import com.ByteKnights.com.resturarent_system.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        ReviewController reviewController = new ReviewController(reviewService);
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getRecentReviews_shouldReturnApiResponseWithReviewList() throws Exception {
        // Arrange
        ReviewResponse response = ReviewResponse.builder()
                .reviewId(1L)
                .rating(5)
                .comment("Great food")
                .createdAt(LocalDateTime.of(2026, 5, 5, 12, 0))
                .build();

        when(reviewService.getRecentReviews()).thenReturn(List.of(response));

        // Act + Assert
        mockMvc.perform(get("/api/v1/reviews/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Recent reviews fetched successfully"))
            .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reviewId").value(1))
                .andExpect(jsonPath("$.data[0].rating").value(5))
                .andExpect(jsonPath("$.data[0].comment").value("Great food"));

        verify(reviewService, times(1)).getRecentReviews();
    }

    @Test
    void submitReview_shouldCallServiceAndReturnSuccessMessage() throws Exception {
        // Arrange
        ReviewSubmissionRequest.OrderReviewRequest orderReview = new ReviewSubmissionRequest.OrderReviewRequest();
        orderReview.setRating(5);
        orderReview.setComment("Excellent");

        ReviewSubmissionRequest request = new ReviewSubmissionRequest();
        request.setOrderReview(orderReview);

        // Act + Assert
        mockMvc.perform(post("/api/v1/orders/30/reviews")
                        .principal(() -> "94770000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review submitted successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(reviewService, times(1)).submitReview(eq("94770000001"), eq(30L), any(ReviewSubmissionRequest.class));
    }
}