package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.CreateOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.OrderResponse;
import com.ByteKnights.com.resturarent_system.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/orders — Create a new online order.
     * Called by the frontend CheckoutPage when the customer clicks "Place Order".
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            OrderResponse response = orderService.createOnlineOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        // TODO: Get orders (filtered by role — customer sees own, staff sees branch)
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        // TODO: Get single order details
        return null;
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id /* TODO: @RequestBody StatusUpdateRequest request */) {
        // TODO: Update order status (staff/manager only)
        return null;
    }
}
