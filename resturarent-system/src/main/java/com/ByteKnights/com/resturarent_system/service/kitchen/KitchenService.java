package com.ByteKnights.com.resturarent_system.service.kitchen;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.InventoryAlertDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PeakHourDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PopularMealDTO;

import java.util.List;

public interface KitchenService {

    KitchenDashboardStatsDTO getKitchenDashboardStats();

    List<PopularMealDTO> getMostPopularMealsInLast7Days();

    List<PeakHourDTO> getPeakHoursInLast7Days();

    List<InventoryAlertDTO> getInventoryAlerts();
}
