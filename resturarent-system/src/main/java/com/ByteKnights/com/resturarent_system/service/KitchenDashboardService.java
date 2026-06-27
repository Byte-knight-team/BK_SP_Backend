package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PeakHourDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PopularMealDTO;

import java.util.List;

public interface KitchenDashboardService {
    KitchenDashboardStatsDTO getKitchenDashboardStats(String userEmail);
    List<PopularMealDTO> getMostPopularMealsInLast7Days(String userEmail);
    List<PeakHourDTO> getPeakHoursInLast7Days(String userEmail);
}
