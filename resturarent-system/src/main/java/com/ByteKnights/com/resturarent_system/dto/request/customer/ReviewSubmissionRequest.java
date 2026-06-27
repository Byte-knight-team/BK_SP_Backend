package com.ByteKnights.com.resturarent_system.dto.request.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * Request body for POST /orders/{orderId}/reviews.
 * Both orderReview and itemReviews are optional — the caller can submit either or both.
 */
@Data
public class ReviewSubmissionRequest {

    @Valid
    private OrderReviewRequest orderReview;

    @Valid
    private List<ItemReviewRequest> itemReviews;

    /** Review for the order as a whole (overall experience). */
    @Data
    public static class OrderReviewRequest {
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        private Integer rating;

        @Size(max = 1000, message = "Comment must not exceed 1000 characters")
        private String comment;

        // S3 keys for images attached to this review section (max 5)
        @Size(max = 5, message = "A maximum of 5 images per review is allowed")
        private List<ReviewImageKeyEntry> imageKeys;
    }

    /** Review for a specific item within the order. */
    @Data
    public static class ItemReviewRequest {
        @NotNull(message = "Order item ID is required")
        private Long orderItemId;

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        private Integer rating;

        @Size(max = 1000, message = "Comment must not exceed 1000 characters")
        private String comment;

        @Size(max = 5, message = "A maximum of 5 images per review is allowed")
        private List<ReviewImageKeyEntry> imageKeys;
    }

    /**
     * Used in the presign step — sent before upload to get a presigned PUT URL.
     * Only file metadata is needed here; the actual binary is uploaded directly to S3.
     */
    @Data
    public static class ReviewImageUploadRequest {
        @NotBlank(message = "File name is required")
        private String fileName;

        @NotBlank(message = "Content type is required")
        private String contentType;
    }

    /**
     * Sent during review submission after images are already uploaded to S3.
     * Carries the real file name and content type alongside the S3 key so the
     * backend stores accurate metadata instead of guessing from the UUID key.
     */
    @Data
    public static class ReviewImageKeyEntry {
        @NotBlank(message = "Object key is required")
        private String objectKey;   // e.g. "reviews/94770000001/uuid.jpg"

        @NotBlank(message = "File name is required")
        private String fileName;    // original name chosen by the user

        @NotBlank(message = "Content type is required")
        private String contentType; // e.g. "image/jpeg"
    }
}
