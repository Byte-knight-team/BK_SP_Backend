package com.ByteKnights.com.resturarent_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    // TODO: Inject OrderService

    @PostMapping
    public ResponseEntity<?> createOrder(/* TODO: @RequestBody OrderRequest request */) {
        // TODO: Place a new order (customer)
        return ResponseEntity.ok("POST /api/orders — not yet implemented");
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        // TODO: Get orders (filtered by role — customer sees own, staff sees branch)
        return ResponseEntity.ok("GET /api/orders — not yet implemented");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        // TODO: Get single order details
        return ResponseEntity.ok("GET /api/orders/" + id + " — not yet implemented");
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id /* TODO: @RequestBody StatusUpdateRequest request */) {
        // TODO: Update order status (staff/manager only)
        return ResponseEntity.ok("PUT /api/orders/" + id + "/status — not yet implemented");
    }
}
