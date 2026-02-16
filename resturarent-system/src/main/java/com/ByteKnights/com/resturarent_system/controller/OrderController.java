package com.ByteKnights.com.resturarent_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final com.ByteKnights.com.resturarent_system.service.OrderService orderService;

    @org.springframework.beans.factory.annotation.Autowired
    public OrderController(com.ByteKnights.com.resturarent_system.service.OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // TODO: createOrder, getOrderById

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, @RequestBody java.util.Map<String, String> payload) {
        String reason = payload.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Cancellation reason is required");
        }
        try {
            com.ByteKnights.com.resturarent_system.dto.OrderDTO cancelledOrder = orderService.cancelOrder(id, reason);
            return ResponseEntity.ok(cancelledOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error cancelling order: " + e.getMessage());
        }
    }
}
