package com.ByteKnights.com.resturarent_system.service.kitchen;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PopularMealDTO;

import java.util.List;

public interface KitchenService {

    KitchenDashboardStatsDTO getKitchenDashboardStats();

    List<PopularMealDTO> getMostPopularMeals();
}
