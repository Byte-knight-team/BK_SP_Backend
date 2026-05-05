package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CancelOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PlaceOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerOrdersPageResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.OrderResponse;
import com.ByteKnights.com.resturarent_system.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import com.ByteKnights.com.resturarent_system.dto.response.customer.OrderPlacementResponse;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderPlacementResponse>> placeOrder(
            Principal principal,
            @RequestBody PlaceOrderRequest request) {

        String userIdentifier = principal != null ? principal.getName() : null;
        OrderPlacementResponse response = orderService.placeCustomerOrder(userIdentifier, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully!", response));
    }

    @PutMapping("/{orderId}/payment")
    public ResponseEntity<ApiResponse<String>> updatePayment(
            @PathVariable Long orderId,
            @RequestBody PaymentUpdateRequest request) {

        orderService.updatePaymentStatus(orderId, request);

        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CustomerOrdersPageResponse>> getMyOrders(
            Principal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userIdentifier = principal.getName();
        CustomerOrdersPageResponse orders = orderService.getCustomerOrdersPage(userIdentifier, type, active, page,
                size);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long orderId,
            Principal principal) {
        String userIdentifier = principal.getName();
        OrderResponse order = orderService.getCustomerOrderById(userIdentifier, orderId);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", order));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody CancelOrderRequest request,
            Principal principal) {
        String userIdentifier = principal.getName();
        orderService.cancelCustomerOrder(userIdentifier, orderId, request.getCancellationReason());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", null));
    }
}