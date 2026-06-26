package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import java.util.List;
import java.util.Optional;

/**
 * Interface defining the contract for business logic operations related to
 * delivery management.
 * 
 * This service handles the core workflows of the delivery system, including:
 * - Fetching orders assigned to delivery staff.
 * - Managing the lifecycle of a delivery (accepting, rejecting, updating
 * status).
 * - Validating that operations are performed by the correct personnel.
 */
public interface DeliveryOrderService {
    /**
     * Retrieves all orders that have been assigned to a specific delivery staff
     * member.
     * 
     * @param userId The ID of the delivery staff member.
     * @return A list of {@link DeliveryOrderDTO} representing the assigned orders.
     */
    List<DeliveryOrderDTO> getAssignedOrders(Long userId);

    /**
     * Retrieves the single active delivery order currently being handled by the
     * staff member.
     * An active order is typically one that has been accepted but not yet
     * completed.
     * 
     * @param userId The ID of the delivery staff member.
     * @return An {@link Optional} containing the {@link DeliveryOrderDTO} if an
     *         active order exists,
     *         or an empty Optional if none is found.
     */
    Optional<DeliveryOrderDTO> getActiveOrder(Long userId);

    /**
     * Allows a delivery staff member to accept an assigned order.
     * 
     * @param orderId The ID of the order to accept.
     * @param userId  The ID of the delivery staff member accepting the order.
     */
    void acceptOrder(Long orderId, Long userId);

    void rejectOrder(Long orderId, Long userId, String reason);

    void updateStatus(Long orderId, Long userId, DeliveryStatus status);
}
