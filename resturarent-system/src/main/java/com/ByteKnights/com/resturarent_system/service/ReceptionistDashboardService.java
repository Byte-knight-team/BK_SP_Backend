package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistRevenuePointDTO;

import java.util.List;

// Same structure as KitchenDashboardService interface
// userEmail comes from Principal so we can resolve which branch the receptionist belongs to
public interface ReceptionistDashboardService {

    // Returns all KPI counts + pipeline breakdown for today
    ReceptionistDashboardStatsDTO getDashboardStats(String userEmail);

    // Returns revenue collected per day for the last 7 days
    List<ReceptionistRevenuePointDTO> getLast7DaysRevenue(String userEmail);
}
