package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.AssignChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.HoldOrderRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.InventoryRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.*;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.service.KitchenService;
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

    //get orderCard details(display all order cards - PENDING,PREPARING,COMPLETED,ON_HOLD)
    @GetMapping("/order-cards")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getOrdersByStatus(@RequestParam OrderStatus status) {
        List<OrderCardDetailsDTO> orders = kitchenService.getOrdersByStatus(status);
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
            @Valid @RequestBody InventoryRequestDTO requestDTO,
            Principal principal) {

        // principal.getName() returns the email of the logged-in user from the JWT token - extract user email from the JWT token's Principal
        String userEmail = principal.getName();

        kitchenService.createRequest(requestDTO, userEmail);

        return new ResponseEntity<>(
                new StandardResponse(201, "Inventory request submitted successfully!", null),
                HttpStatus.CREATED
        );
    }

    //update current item count in the inventory
    @PutMapping("/inventory/update")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> updateInventoryStock(

            @Valid @RequestBody UpdateStockDTO updateDTO) {

        kitchenService.updateInventoryStock(updateDTO);

        return new ResponseEntity<>(
                new StandardResponse(200, "Stock updated successfully!", null),
                HttpStatus.OK
        );
    }

    //get all details of a specific order by order id (display in the orders page)
    @GetMapping("/order-details/{id}")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getOrderDetails(@PathVariable Long id) {

        // call the service to get the specific order details
        OrderDetailsDTO orderDetails = kitchenService.getOrderDetails(id);

        return new ResponseEntity<>(
                new StandardResponse(200, "Success", orderDetails),
                HttpStatus.OK
        );
    }

    // Get Line Chefs for assign them to prepare meals
    @GetMapping("/available-chefs")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getAvailableChefsForAssignment(Principal principal) {

        // Extract email from the JWT token
        String userEmail = principal.getName();

        List<ChefAssignDTO> chefs = kitchenService.getAvailableChefsForAssignment(userEmail);

        return new ResponseEntity<>(
                new StandardResponse(200, "Success", chefs),
                HttpStatus.OK
        );
    }

    // Assign a chef to a specific meal
    @PutMapping("/order-items/{itemId}/assign")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> assignChefToMeal(
            @PathVariable Long itemId,
            @RequestBody AssignChefRequestDTO requestDTO) {

        kitchenService.assignChefToMeal(itemId, requestDTO.getChefStaffId());

        return new ResponseEntity<>(
                new StandardResponse(200, "Chef assigned successfully", null),
                HttpStatus.OK
        );
    }

    // get all the Line Chefs Who is not checked in yet for the day and display them in the check-in list of the kitchen dashboard
    // then the chef can check in from there
    @GetMapping("/chefs/check-in-list")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getChefsForCheckIn(Principal principal) {

        List<ChefCheckInDTO> chefList = kitchenService.getLineChefsForCheckIn(principal.getName());

        return new ResponseEntity<>(
                new StandardResponse(200, "success", chefList),
                HttpStatus.OK
        );
    }

    // create an attendance record for the chef when they check in for the day from the kitchen dashboard
    @PostMapping("/chefs/{chefId}/check-in")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> checkInChef(@PathVariable Long chefId) {

        kitchenService.checkInChef(chefId);

        return new ResponseEntity<>(
                new StandardResponse(200, "Chef checked in successfully", null),
                HttpStatus.OK
        );
    }

    // check out a chef
    @PostMapping("/chefs/{chefId}/check-out")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> checkOutChef(@PathVariable Long chefId) {

        kitchenService.checkOutChef(chefId);

        return new ResponseEntity<>(
                new StandardResponse(200, "Chef checked out successfully", null),
                HttpStatus.OK
        );
    }

    // hold a pending order.
    @PutMapping("/orders/{orderId}/hold")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> holdOrder(
            @PathVariable Long orderId,
            @RequestBody HoldOrderRequestDTO requestDTO) {

        kitchenService.holdOrder(orderId, requestDTO.getHoldReason());

        return new ResponseEntity<>(
                new StandardResponse(200, "Order put on hold successfully", null),
                HttpStatus.OK
        );
    }

    // update the status of a specific meal item to "PREPARING" when the chef starts preparing it
    // at the same time the order status will update as PREPARING and chef working status becomes COOKING
    @PutMapping("/order-items/{itemId}/start")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> startMeal(@PathVariable Long itemId) {

        kitchenService.startMeal(itemId);

        return new ResponseEntity<>(
                new StandardResponse(200, "Meal preparation started", null),
                HttpStatus.OK
        );
    }

    // update meal status as completed and return current order status
    @PutMapping("/order-items/{itemId}/complete")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> completeMeal(@PathVariable Long itemId) {

        MealCompletionResponseDTO orderStats = kitchenService.completeMeal(itemId);

        return new ResponseEntity<>(
                new StandardResponse(200, "Success", orderStats),
                HttpStatus.OK
        );
    }

    // get all details of all line chefs today
    @GetMapping("/chefs/today-details")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<StandardResponse> getChefDetailsToday(Principal principal) {

        List<ChefDetailsDTO> chefs = kitchenService.getChefDetailsToday(principal.getName());

        return new ResponseEntity<>(
                new StandardResponse(200, "Success", chefs),
                HttpStatus.OK
        );
    }



}
