package com.ByteKnights.com.resturarent_system.service.kitchen;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.*;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;

import java.util.List;

public interface KitchenService {

    KitchenDashboardStatsDTO getKitchenDashboardStats();

    List<PopularMealDTO> getMostPopularMealsInLast7Days();

    List<PeakHourDTO> getPeakHoursInLast7Days();

    List<InventoryAlertDTO> getInventoryAlerts();

    List<KitchenOrderDTO> getOrdersByStatus(OrderStatus status);

    List<InventoryAlertDTO> getAllInventoryItems();
}
