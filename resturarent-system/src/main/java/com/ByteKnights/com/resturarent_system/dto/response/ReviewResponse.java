package com.ByteKnights.com.resturarent_system.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long reviewId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
