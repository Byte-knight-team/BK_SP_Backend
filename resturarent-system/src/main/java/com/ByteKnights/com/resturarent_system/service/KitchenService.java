package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.InventoryRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.*;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import jakarta.validation.Valid;

import java.util.List;

public interface KitchenService {

    KitchenDashboardStatsDTO getKitchenDashboardStats();

    List<PopularMealDTO> getMostPopularMealsInLast7Days();

    List<PeakHourDTO> getPeakHoursInLast7Days();

    List<InventoryDetailsDTO> getInventoryAlerts();

    List<OrderCardDetailsDTO> getOrdersByStatus(OrderStatus status);

    List<InventoryDetailsDTO> getAllInventoryItems();

    void createRequest(@Valid InventoryRequestDTO requestDTO, String userEmail);

    void updateInventoryStock(@Valid UpdateStockDTO updateDTO);

    OrderDetailsDTO getOrderDetails(Long id);

    List<ChefAssignDTO> getAvailableChefsForAssignment(String userEmail);

    void assignChefToMeal(Long itemId, Long chefId);

    List<ChefCheckInDTO> getLineChefsForCheckIn(String name);

    void checkInChef(Long chefId);

    void checkOutChef(Long chefId);

    void holdOrder(Long orderId, String holdReason);

    void startMeal(Long itemId);

    MealCompletionResponseDTO completeMeal(Long itemId);

    List<ChefDetailsDTO> getChefDetailsToday(String name);
}
