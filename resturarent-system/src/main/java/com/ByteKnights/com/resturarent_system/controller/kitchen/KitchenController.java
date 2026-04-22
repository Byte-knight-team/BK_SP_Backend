package com.ByteKnights.com.resturarent_system.controller.kitchen;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.*;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.service.kitchen.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/kitchen")
@CrossOrigin
@RequiredArgsConstructor
public class KitchenController {

    private final KitchenService kitchenService;

    //get kitchen dashboard stat data
    @GetMapping("/stats")
    public ResponseEntity<StandardResponse> getKitchenDashboardStats() {
        KitchenDashboardStatsDTO stats = kitchenService.getKitchenDashboardStats();

        return new ResponseEntity<>(
                new StandardResponse(200, "Success", stats),
                HttpStatus.OK
        );
    }

    //get most popular meals data
    @GetMapping("/popular-meals")
    public ResponseEntity<StandardResponse> getMostPopularMeals() {
        List<PopularMealDTO> popularMeals = kitchenService.getMostPopularMealsInLast7Days();
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", popularMeals),
                HttpStatus.OK
        );
    }

    //get peak hours data
    @GetMapping("/peak-hours")
    public ResponseEntity<StandardResponse> getPeakHours() {
        List<PeakHourDTO> peakHours = kitchenService.getPeakHoursInLast7Days();
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", peakHours),
                HttpStatus.OK
        );
    }

    //get inventory alerts data
    @GetMapping("/inventory-alerts")
    public ResponseEntity<StandardResponse> getInventoryAlerts() {
        List<InventoryAlertDTO> alerts = kitchenService.getInventoryAlerts();
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", alerts),
                HttpStatus.OK
        );
    }

    //get orders by status
    @GetMapping("/orders")
    public ResponseEntity<StandardResponse> getOrdersByStatus(@RequestParam OrderStatus status) {
        List<KitchenOrderDTO> orders = kitchenService.getOrdersByStatus(status);
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", orders),
                HttpStatus.OK
        );
    }


}


