package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for broadcasting real-time notifications to connected clients
 * via WebSocket (STOMP protocol).
 *
 * Usage: Inject this service anywhere you need to push data to the frontend.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast a new kitchen alert to all receptionist clients in the same branch.
     *
     * Topic: /topic/branch/{branchId}/alerts
     * Subscribers: Receptionist dashboard
     *
     * @param branchId   The branch ID to scope the broadcast (no cross-branch leaks)
     * @param alertDTO   The alert data to push
     */
    public void broadcastKitchenAlert(Long branchId, ActiveAlertDTO alertDTO) {
        String destination = "/topic/branch/" + branchId + "/alerts";
        log.info("Broadcasting kitchen alert to {}: {}", destination, alertDTO.getMessage());
        messagingTemplate.convertAndSend(destination, alertDTO);
    }

    /**
     * Broadcast a new order notification to all kitchen clients in the same branch.
     *
     * Topic: /topic/branch/{branchId}/kitchen-orders
     * Subscribers: Kitchen order management page
     *
     * @param branchId    The branch ID to scope the broadcast
     * @param orderNumber The order number e.g. "ORD-CD5C6E"
     */
    public void broadcastNewKitchenOrder(Long branchId, String orderNumber) {
        String destination = "/topic/branch/" + branchId + "/kitchen-orders";
        java.util.Map<String, String> payload = java.util.Map.of(
                "orderNumber", orderNumber,
                "message", "New order received: " + orderNumber
        );
        log.info("Broadcasting new kitchen order to {}: {}", destination, orderNumber);
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Notify a specific line chef that a new item has been assigned to them.
     *
     * Topic: /topic/line-chef/{lineChefUserId}/new-item
     * Subscribers: Line chef dashboard
     *
     * @param lineChefUserId  The user ID of the line chef (for per-user topic scoping)
     * @param orderNumber     The order the item belongs to
     * @param itemName        The name of the assigned item
     */
    /**
     * Notify the kitchen (chief chef) that a line chef has updated an item status.
     *
     * Topic: /topic/branch/{branchId}/kitchen-item-update
     * Subscribers: Kitchen order management page (SelectedOrder panel)
     *
     * @param branchId    The branch ID to scope the broadcast
     * @param orderId     The order that contains the updated item
     * @param itemName    The name of the item
     * @param newStatus   The new status (PREPARING or READY)
     */
    public void broadcastKitchenItemUpdate(Long branchId, Long orderId, String itemName, String newStatus) {
        String destination = "/topic/branch/" + branchId + "/kitchen-item-update";
        java.util.Map<String, String> payload = java.util.Map.of(
                "orderId", String.valueOf(orderId),
                "itemName", itemName,
                "newStatus", newStatus
        );
        log.info("Broadcasting item update to kitchen {}: {} -> {}", destination, itemName, newStatus);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void broadcastLineChefItemAssigned(Long lineChefUserId, String orderNumber, String itemName) {
        String destination = "/topic/line-chef/" + lineChefUserId + "/new-item";
        java.util.Map<String, String> payload = java.util.Map.of(
                "orderNumber", orderNumber,
                "itemName", itemName,
                "message", "New item assigned: " + itemName + " (Order " + orderNumber + ")"
        );
        log.info("Broadcasting item assignment to line chef {}: {}", lineChefUserId, itemName);
        messagingTemplate.convertAndSend(destination, payload);
    }

}
