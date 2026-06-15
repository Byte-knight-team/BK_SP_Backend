package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatisticsResponse {

    // Financial
    private BigDecimal totalLifetimeSpend;
    private BigDecimal totalDiscountsSaved;

    // Spending Trend (6 months)
    private List<MonthlySpend> spendingTrend;

    // Top 3 Most Ordered Items
    private List<TopItem> topItems;

    // Order Type Breakdown
    private Long qrOrderCount;
    private Long deliveryOrderCount;
    private Long pickupOrderCount;

    // Loyalty
    private Integer currentLoyaltyPoints;
    private Integer totalPointsEarned;
    private Integer totalPointsRedeemed;

    // Account
    private LocalDateTime memberSince;
    private Long totalItemsOrdered;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySpend {
        private String month;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopItem {
        private String name;
        private String imageUrl;
        private Long orderCount;
    }
}
