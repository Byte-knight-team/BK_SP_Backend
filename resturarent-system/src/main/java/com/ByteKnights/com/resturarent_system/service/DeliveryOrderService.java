package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import java.util.List;
import java.util.Optional;

/**
 * Interface defining the contract for business logic operations related to
 * delivery management.
 * 
 * This service handles the core workflows of the delivery system
 */

public interface DeliveryOrderService {
    /**
     * Retrieves all orders that have been assigned to a specific delivery staff
     * member.
     */
    List<DeliveryOrderDTO> getAssignedOrders(Long userId);

    /**
     * Retrieves the single active delivery order currently being handled by the
     * staff member.
     * An active order is typically one that has been accepted but not yet
     * completed.
     */
    Optional<DeliveryOrderDTO> getActiveOrder(Long userId);

    /**
     * Allows a delivery staff member to accept,reject and update an assigned order.
     */
    void acceptOrder(Long orderId, Long userId);

    void rejectOrder(Long orderId, Long userId, String reason);

    void updateStatus(Long orderId, Long userId, DeliveryStatus status);
}
