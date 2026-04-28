package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import java.util.List;
import java.util.Optional;

public interface DeliveryOrderService {
    List<DeliveryOrderDTO> getAssignedOrders(Long userId);

    Optional<DeliveryOrderDTO> getActiveOrder(Long userId);

    void acceptOrder(Long orderId, Long userId);

    void rejectOrder(Long orderId, Long userId, String reason);

    void updateStatus(Long orderId, Long userId, DeliveryStatus status);
}
