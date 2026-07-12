package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
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
    private final OrderRepository orderRepository;

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
     * Notify the branch that a kitchen alert/issue has been resolved (same /alerts topic).
     * The receptionist clients toast it and refresh their active-alert count.
     */
    public void broadcastKitchenAlertResolved(Long branchId, String message) {
        String destination = "/topic/branch/" + branchId + "/alerts";
        log.info("Broadcasting kitchen alert RESOLVED to {}: {}", destination, message);
        messagingTemplate.convertAndSend(destination, java.util.Map.of(
                "type", "RESOLVED",
                "message", message
        ));
    }

    /**
     * Broadcast a new order notification to all receptionist clients in the branch.
     *
     * Topic: /topic/branch/{branchId}/new-order
     * Subscribers: Receptionist dashboard (Notifier)
     *
     * @param branchId    The branch ID to scope the broadcast
     * @param orderNumber The order number e.g. "ORD-CD5C6E"
     * @param orderType   The order type e.g. "ONLINE_DELIVERY"
     * @param orderId     The order ID
     */
    public void broadcastNewReceptionistOrder(Long branchId, String orderNumber, String orderType, Long orderId) {
        String destination = "/topic/branch/" + branchId + "/new-order";
        java.util.Map<String, String> payload = java.util.Map.of(
                "orderNumber", orderNumber,
                "orderType", orderType,
                "orderId", String.valueOf(orderId)
        );
        log.info("Broadcasting new receptionist order to {}: {}", destination, orderNumber);
        messagingTemplate.convertAndSend(destination, payload);
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
     * Notify all receptionist clients in a branch that table data has changed.
     * Called when: a table is occupied/cleared, or a QR order is placed/completed.
     *
     * Topic: /topic/branch/{branchId}/table-update
     * Subscribers: Receptionist Table Management page
     */
    /**
     * Broadcast a reservation reminder to the receptionist.
     * type: REMINDER_1HR or REMINDER_15MIN
     *
     * Topic: /topic/branch/{branchId}/reservation-reminder
     * Subscribers: Receptionist Table Management page
     */
    public void broadcastReservationReminder(Long branchId, String type, Integer tableNumber, String reservationTime) {
        String destination = "/topic/branch/" + branchId + "/reservation-reminder";
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("type", type);
        payload.put("tableNumber", String.valueOf(tableNumber));
        payload.put("reservationTime", reservationTime);
        log.info("Broadcasting reservation reminder [{}] to branch {}, table {}", type, branchId, tableNumber);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void broadcastTableUpdate(Long branchId) {
        String destination = "/topic/branch/" + branchId + "/table-update";
        messagingTemplate.convertAndSend(destination, java.util.Map.of("branchId", String.valueOf(branchId)));
        log.info("Broadcasting table update to {}", destination);
    }

    /**
     * Broadcast an order status update to:
     *   1. /topic/order/{orderId}/status   → Order Confirmation page (real-time timeline)
     *   2. /topic/user/{userId}/orders     → Global toast notifications (menu, orders, profile, cart, etc.)
     *
     * The user-level broadcast is resolved internally via OrderRepository,
     * so callers only need to pass orderId and newStatus.
     *
     * @param orderId   The order ID
     * @param newStatus The new status of the order (e.g. "PREPARING")
     */
    public void broadcastOrderStatusUpdate(Long orderId, String newStatus) {
        // 1. Per-order topic (Order Confirmation page real-time timeline)
        String orderDestination = "/topic/order/" + orderId + "/status";
        log.info("Broadcasting order status update to {}: {}", orderDestination, newStatus);

        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("orderId", String.valueOf(orderId));
        payload.put("orderStatus", newStatus);

        messagingTemplate.convertAndSend(orderDestination, payload);

        // 2. Global user-level topic (toast notifications across all customer pages)
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null && order.getCustomer() != null && order.getCustomer().getUser() != null) {
                Long userId = order.getCustomer().getUser().getId();
                String orderNumber = order.getOrderNumber();

                String userDestination = "/topic/user/" + userId + "/orders";
                log.info("Broadcasting global order update to {}: {} -> {}", userDestination, orderNumber, newStatus);

                java.util.Map<String, String> globalPayload = new java.util.HashMap<>();
                globalPayload.put("orderId", String.valueOf(orderId));
                globalPayload.put("orderNumber", orderNumber != null ? orderNumber : "");
                globalPayload.put("orderStatus", newStatus);

                messagingTemplate.convertAndSend(userDestination, globalPayload);
            }
        } catch (Exception e) {
            // Don't let global notification failure break the main flow
            log.warn("Failed to broadcast global order update for order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Notify receptionist clients that reservation data has changed (created or cancelled).
     *
     * Topic: /topic/branch/{branchId}/reservation-update
     * Subscribers: Receptionist dashboard (UpcomingReservationsCard)
     */
    public void broadcastReservationUpdate(Long branchId) {
        String destination = "/topic/branch/" + branchId + "/reservation-update";
        messagingTemplate.convertAndSend(destination, java.util.Map.of("branchId", String.valueOf(branchId)));
        log.info("Broadcasting reservation update to {}", destination);
    }

    /**
     * Broadcast a new reservation request notification to receptionist.
     * Topic: /topic/branch/{branchId}/new-reservation
     */
    public void broadcastNewReservationRequest(Long branchId, Long reservationId) {
        String destination = "/topic/branch/" + branchId + "/new-reservation";
        messagingTemplate.convertAndSend(destination, java.util.Map.of(
                "reservationId", String.valueOf(reservationId)
        ));
        log.info("Broadcasting new reservation request to {}", destination);
    }

    /**
     * Broadcast reservation status updates to the customer.
     * Topic: /topic/user/{userId}/reservations
     */
    public void broadcastReservationStatusToCustomer(Long userId, Long reservationId, String newStatus) {
        String destination = "/topic/user/" + userId + "/reservations";
        messagingTemplate.convertAndSend(destination, java.util.Map.of(
                "reservationId", String.valueOf(reservationId),
                "reservationStatus", newStatus
        ));
        log.info("Broadcasting global reservation update to {}: {}", destination, newStatus);
    }
}
