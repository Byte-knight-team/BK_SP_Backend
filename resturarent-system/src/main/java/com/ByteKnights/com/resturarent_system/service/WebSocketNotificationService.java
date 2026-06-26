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

}
