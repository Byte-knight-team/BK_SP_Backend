package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.AssignChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.HoldOrderRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.MealCompletionResponseDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderCardDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderDetailsDTO;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.service.KitchenOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kitchen")
@CrossOrigin
@RequiredArgsConstructor
public class KitchenOrderController {

    private final KitchenOrderService kitchenOrderService;

    @GetMapping("/order-cards")
    @PreAuthorize("hasAuthority('KITCHEN_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getOrdersByStatus(
            @RequestParam OrderStatus status,
            Principal principal) {
        List<OrderCardDetailsDTO> orders = kitchenOrderService.getOrdersByStatus(status, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", orders), HttpStatus.OK);
    }

    @GetMapping("/order-details/{id}")
    @PreAuthorize("hasAuthority('KITCHEN_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getOrderDetails(
            @PathVariable Long id,
            Principal principal) {
        OrderDetailsDTO orderDetails = kitchenOrderService.getOrderDetails(id, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", orderDetails), HttpStatus.OK);
    }

    @PutMapping("/order-items/{itemId}/assign")
    @PreAuthorize("hasAuthority('KITCHEN_ORDER_ASSIGN')")
    public ResponseEntity<StandardResponse> assignChefToMeal(
            @PathVariable Long itemId,
            @RequestBody AssignChefRequestDTO requestDTO) {
        kitchenOrderService.assignChefToMeal(itemId, requestDTO.getChefStaffId());
        return new ResponseEntity<>(new StandardResponse(200, "Chef assigned successfully", null), HttpStatus.OK);
    }

    @PutMapping("/order-items/{itemId}/start")
    @PreAuthorize("hasAuthority('KITCHEN_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> startMeal(@PathVariable Long itemId) {
        kitchenOrderService.startMeal(itemId);
        return new ResponseEntity<>(new StandardResponse(200, "Meal preparation started", null), HttpStatus.OK);
    }

    @PutMapping("/order-items/{itemId}/complete")
    @PreAuthorize("hasAuthority('KITCHEN_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> completeMeal(@PathVariable Long itemId) {
        MealCompletionResponseDTO orderStats = kitchenOrderService.completeMeal(itemId);
        return new ResponseEntity<>(new StandardResponse(200, "Success", orderStats), HttpStatus.OK);
    }

    @PutMapping("/orders/{orderId}/hold")
    @PreAuthorize("hasAuthority('KITCHEN_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> holdOrder(
            @PathVariable Long orderId,
            @RequestBody HoldOrderRequestDTO requestDTO,
            Principal principal) {
        kitchenOrderService.holdOrder(orderId, requestDTO.getHoldReason(), principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Order put on hold successfully", null), HttpStatus.OK);
    }
}
