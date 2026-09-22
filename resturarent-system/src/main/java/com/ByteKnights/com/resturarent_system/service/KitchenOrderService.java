package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenOrderHistoryDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderCardDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.PagedResponse;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;

import java.util.List;

public interface KitchenOrderService {
    List<OrderCardDetailsDTO> getOrdersByStatus(OrderStatus status, String userEmail);
    OrderDetailsDTO getOrderDetails(Long orderId, String userEmail);
    void assignChefToMeal(Long itemId, Long chefId);
    void holdOrder(Long orderId, String holdReason, String userEmail);
    PagedResponse<KitchenOrderHistoryDTO> getOrderHistory(String userEmail, int page, int size, String date, String status);
}
