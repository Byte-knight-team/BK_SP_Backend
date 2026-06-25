package com.ByteKnights.com.resturarent_system.dto.response.customer;

import com.ByteKnights.com.resturarent_system.dto.response.ReviewResponse;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Full response for the menu item reviews modal.
 * Combines the rating summary with a paginated list of individual reviews.
 */
@Data
@Builder
public class MenuItemReviewsResponse {
    private MenuItemReviewSummaryResponse summary;
    private List<ReviewResponse> reviews;
    private int page;
    private int totalPages;
    private boolean hasMore;
}
