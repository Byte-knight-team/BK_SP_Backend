package com.ByteKnights.com.resturarent_system.dto.request.customer;

import lombok.Data;
import java.util.List;

@Data
public class ReviewSubmissionRequest {
    
    private OrderReviewRequest orderReview;
    private List<ItemReviewRequest> itemReviews;

    @Data
    public static class OrderReviewRequest {
        private Integer rating;
        private String comment;
    }

    @Data
    public static class ItemReviewRequest {
        private Long orderItemId;
        private Integer rating;
        private String comment;
    }
}
