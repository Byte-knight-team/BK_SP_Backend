package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for broadcasting real-time notifications to connected
 * clients
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
     * @param branchId The branch ID to scope the broadcast (no cross-branch leaks)
     * @param alertDTO The alert data to push
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
     * Notify the kitchen (chief chef) that a line chef has updated an item status.
     *
     * Topic: /topic/branch/{branchId}/kitchen-item-update
     * Subscribers: Kitchen order management page (SelectedOrder panel)
     *
     * @param branchId       The branch ID to scope the broadcast
     * @param orderId        The order that contains the updated item
     * @param itemName       The name of the item
     * @param newItemStatus  The new item status (PREPARING or READY)
     * @param newOrderStatus The new order status
     */
    public void broadcastKitchenItemUpdate(Long branchId, Long orderId, String orderNumber, String itemName, String newItemStatus, String newOrderStatus, String orderType) {
        String destination = "/topic/branch/" + branchId + "/kitchen-item-update";
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("orderId", String.valueOf(orderId));
        payload.put("orderNumber", orderNumber);
        payload.put("itemName", itemName);
        payload.put("newStatus", newItemStatus);
        payload.put("orderStatus", newOrderStatus);
        payload.put("orderType", orderType);
        log.info("Broadcasting item update to kitchen {}: {} -> {} (order: {})", destination, itemName, newItemStatus, newOrderStatus);
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Notify a specific line chef that a new item has been assigned to them.
     *
     * Topic: /topic/line-chef/{lineChefUserId}/new-item
     * Subscribers: Line chef dashboard
     */
    public void broadcastLineChefItemAssigned(Long lineChefUserId, String orderNumber, String itemName, String kitchenNotes) {
        String destination = "/topic/line-chef/" + lineChefUserId + "/new-item";
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("orderNumber", orderNumber);
        payload.put("itemName", itemName);
        payload.put("message", "New item assigned: " + itemName + " (Order " + orderNumber + ")");
        payload.put("kitchenNotes", kitchenNotes);
        log.info("Broadcasting item assignment to line chef {}: {}", lineChefUserId, itemName);
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Notify both kitchen and receptionist that an order's status changed cross-role.
     * Topic: /topic/branch/{branchId}/order-status-update
     * Used for: kitchen holds order (→ receptionist), receptionist sends back (→ kitchen)
     */
    public void broadcastOrderStatusChanged(Long branchId, Long orderId, String orderNumber, String newStatus) {
        String destination = "/topic/branch/" + branchId + "/order-status-update";
        messagingTemplate.convertAndSend(destination, java.util.Map.of(
                "orderId", String.valueOf(orderId),
                "orderNumber", orderNumber,
                "newStatus", newStatus
        ));
    }

    /**
     * Notify a line chef that their assigned item has been reassigned to another chef.
     *
     * Topic: /topic/line-chef/{lineChefUserId}/item-removed
     * Subscribers: Line chef dashboard
     */
    public void broadcastLineChefItemRemoved(Long lineChefUserId, String itemName, String orderNumber, String newChefName) {
        String destination = "/topic/line-chef/" + lineChefUserId + "/item-removed";
        java.util.Map<String, String> payload = java.util.Map.of(
                "itemName", itemName,
                "orderNumber", orderNumber,
                "newChefName", newChefName
        );
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Broadcast an order status update to a specific customer's order topic.
     *
     * Topic: /topic/order/{orderId}/status
     * Subscribers: Customer Order Confirmation Page
     *
     * @param orderId   The order ID to scope the broadcast
     * @param newStatus The new status of the order (e.g. "PREPARING")
     */
    public void broadcastOrderStatusUpdate(Long orderId, String newStatus) {
        String destination = "/topic/order/" + orderId + "/status";
        log.info("Broadcasting order status update to {}: {}", destination, newStatus);

        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("orderId", String.valueOf(orderId));
        payload.put("orderStatus", newStatus);

        messagingTemplate.convertAndSend(destination, payload);
    }
}
