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
        
        // Wrap the status in a simple JSON structure
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("orderId", String.valueOf(orderId));
        payload.put("orderStatus", newStatus);
        
        messagingTemplate.convertAndSend(destination, payload);
    }
}
