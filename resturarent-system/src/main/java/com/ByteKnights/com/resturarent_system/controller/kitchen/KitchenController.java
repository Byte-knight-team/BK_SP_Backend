package com.ByteKnights.com.resturarent_system.controller.kitchen;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.*;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.service.kitchen.KitchenService;
import jakarta.validation.Valid;
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
public class KitchenController {

    private final KitchenService kitchenService;

    //get kitchen dashboard stat data
    @GetMapping("/stats")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getKitchenDashboardStats() {
        KitchenDashboardStatsDTO stats = kitchenService.getKitchenDashboardStats();

        return new ResponseEntity<>(
                new StandardResponse(200, "Success", stats),
                HttpStatus.OK
        );
    }

    //get most popular meals data
    @GetMapping("/popular-meals")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getMostPopularMeals() {
        List<PopularMealDTO> popularMeals = kitchenService.getMostPopularMealsInLast7Days();
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", popularMeals),
                HttpStatus.OK
        );
    }

    //get peak hours data
    @GetMapping("/peak-hours")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getPeakHours() {
        List<PeakHourDTO> peakHours = kitchenService.getPeakHoursInLast7Days();
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", peakHours),
                HttpStatus.OK
        );
    }

    //get inventory alerts data
    @GetMapping("/inventory-alerts")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getInventoryAlerts() {
        List<InventoryDetailsDTO> alerts = kitchenService.getInventoryAlerts();
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", alerts),
                HttpStatus.OK
        );
    }

    //get orders by status
    @GetMapping("/orders")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getOrdersByStatus(@RequestParam OrderStatus status) {
        List<KitchenOrderDTO> orders = kitchenService.getOrdersByStatus(status);
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", orders),
                HttpStatus.OK
        );
    }

    //get all inventory details
    @GetMapping("/inventory/all")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getAllInventory() {
        List<InventoryDetailsDTO> items = kitchenService.getAllInventoryItems();
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", items),
                HttpStatus.OK
        );
    }

    // inventory requests submitted by Chefs.
    // one endpoint for both request types ("REFILL_STOCK", "ADD_NEW_ITEM")
    @PostMapping("/inventory/request")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> createRequest(
            @Valid @RequestBody CreateChefRequestDTO requestDTO,
            Principal principal) {

        // principal.getName() returns the email of the logged-in user from the JWT token - extract user email from the JWT token's Principal
        String userEmail = principal.getName();

        kitchenService.createRequest(requestDTO, userEmail);

        return new ResponseEntity<>(
                new StandardResponse(201, "Inventory request submitted successfully!", null),
                HttpStatus.CREATED
        );
    }

}


