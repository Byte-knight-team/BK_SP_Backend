package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import java.util.List;

public interface DeliveryOrderService {
    List<DeliveryOrderDTO> getAssignedOrders(Long userId);
    Optional<DeliveryOrderDTO> getActiveOrder(Long userId);
    void acceptOrder(Long orderId, Long userId);
    void rejectOrder(Long orderId, Long userId, String reason);
}
