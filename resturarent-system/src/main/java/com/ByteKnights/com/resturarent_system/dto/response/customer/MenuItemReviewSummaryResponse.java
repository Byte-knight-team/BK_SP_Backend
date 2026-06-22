package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * Rating summary for a menu item: average, total count, and per-star breakdown.
 * Used at the top of the reviews modal to show the bar chart.
 */
@Data
@Builder
public class MenuItemReviewSummaryResponse {
    private Long menuItemId;
    private Double averageRating;
    private Long totalCount;

    // key = star value (1-5), value = number of reviews with that rating
    private Map<Integer, Long> ratingBreakdown;
}
