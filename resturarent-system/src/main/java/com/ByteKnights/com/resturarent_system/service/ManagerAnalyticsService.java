package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.manager.analytics.AnalyticsSummaryDTO;
import java.time.LocalDate;

/**
 * Service interface for handling complex branch analytics and reporting logic.
 */
public interface ManagerAnalyticsService {
    
    /**
     * Aggregates and returns a comprehensive analytics summary for a specific branch.
     * 
     * @param branchId  The ID of the branch to analyze (optional for branch-scoped users).
     * @param userId    The ID of the user requesting the data (for security validation).
     * @param startDate The start date for the analysis period.
     * @param endDate   The end date for the analysis period.
     * @return AnalyticsSummaryDTO containing aggregated metrics and trends.
     */
    AnalyticsSummaryDTO getBranchAnalyticsSummary(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);
}
