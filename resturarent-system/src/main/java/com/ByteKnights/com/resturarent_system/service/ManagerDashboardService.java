package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerDashboardSummaryDTO;

public interface ManagerDashboardService {
    
    /**
     * Retrieves the dashboard summary metrics for a specific branch.
     *
     * @param targetBranchId Optional branch ID if super admin is viewing.
     * @param userId         The ID of the currently authenticated user.
     * @return ManagerDashboardSummaryDTO containing all required dashboard metrics.
     */
    ManagerDashboardSummaryDTO getDashboardSummary(Long targetBranchId, Long userId);
}
