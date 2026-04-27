package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardOrderFlowResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardRevenuePointResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardSummaryResponse;

import java.util.List;

public interface AdminDashboardService {

    AdminDashboardSummaryResponse getSummary();

    AdminDashboardOrderFlowResponse getOrderFlowSummary();

    List<AdminDashboardRevenuePointResponse> getRevenueTrend(int days);
}
