package com.ByteKnights.com.resturarent_system.dto.response;

import com.ByteKnights.com.resturarent_system.dto.response.customer.ReviewImageResponse;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReviewResponse {
    private Long reviewId;
    private String customerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private List<ReviewImageResponse> images;
}
