package com.ByteKnights.com.resturarent_system.service.kitchen;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.*;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import jakarta.validation.Valid;

import java.util.List;

public interface KitchenService {

    KitchenDashboardStatsDTO getKitchenDashboardStats();

    List<PopularMealDTO> getMostPopularMealsInLast7Days();

    List<PeakHourDTO> getPeakHoursInLast7Days();

    List<InventoryDetailsDTO> getInventoryAlerts();

    List<KitchenOrderDTO> getOrdersByStatus(OrderStatus status);

    List<InventoryDetailsDTO> getAllInventoryItems();

    void createRequest(@Valid CreateChefRequestDTO requestDTO, String userEmail);
}
