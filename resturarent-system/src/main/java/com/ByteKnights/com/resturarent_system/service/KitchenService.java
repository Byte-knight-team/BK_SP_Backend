package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateAlertRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.InventoryRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.*;
import com.ByteKnights.com.resturarent_system.entity.ChefWorkStatus;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import jakarta.validation.Valid;

import java.util.List;

public interface KitchenService {

    KitchenDashboardStatsDTO getKitchenDashboardStats(String userEmail);

    List<PopularMealDTO> getMostPopularMealsInLast7Days(String userEmail);

    List<PeakHourDTO> getPeakHoursInLast7Days();

    List<InventoryDetailsDTO> getInventoryAlerts(String userEmail);

    List<OrderCardDetailsDTO> getOrdersByStatus(OrderStatus status, String userEmail);

    List<InventoryDetailsDTO> getAllInventoryItems(String userEmail);

    void createRequest(@Valid InventoryRequestDTO requestDTO, String userEmail);

    void updateInventoryStock(@Valid UpdateStockDTO updateDTO);

    OrderDetailsDTO getOrderDetails(Long id);

    List<ChefAssignDTO> getAvailableChefsForAssignment(String userEmail);

    void assignChefToMeal(Long itemId, Long chefId);

    void checkInChef(Long chefId);

    void checkOutChef(Long chefId);

    void holdOrder(Long orderId, String holdReason);

    void startMeal(Long itemId);

    MealCompletionResponseDTO completeMeal(Long itemId);

    List<ChefDetailsDTO> getChefDetailsToday(String name);

    void updateChefWorkStatus(Long chefId, ChefWorkStatus newStatus);

    void createKitchenAlert(@Valid CreateAlertRequestDTO requestDTO, String name);

    List<ActiveAlertDTO> getActiveAlerts(String name);

    void resolveAlert(Long id, String name);
}
