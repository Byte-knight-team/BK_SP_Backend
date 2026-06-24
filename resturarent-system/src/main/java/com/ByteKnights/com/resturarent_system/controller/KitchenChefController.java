package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateWorkStatusDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ChefDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ChefDetailsDTO;
import com.ByteKnights.com.resturarent_system.service.KitchenChefService;
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
public class KitchenChefController {

    private final KitchenChefService kitchenChefService;

    @GetMapping("/chefs/stats")
    @PreAuthorize("hasAuthority('KITCHEN_VIEW_STATS')")
    public ResponseEntity<StandardResponse> getChefDashboardStats(Principal principal) {
        ChefDashboardStatsDTO stats = kitchenChefService.getChefDashboardStats(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", stats), HttpStatus.OK);
    }

    @GetMapping("/chefs/today-details")
    @PreAuthorize("hasAuthority('KITCHEN_CHEF_MANAGE')")
    public ResponseEntity<StandardResponse> getChefDetailsToday(Principal principal) {
        List<ChefDetailsDTO> chefs = kitchenChefService.getChefDetailsToday(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", chefs), HttpStatus.OK);
    }

    @PostMapping("/chefs/{chefId}/check-in")
    @PreAuthorize("hasAuthority('KITCHEN_CHEF_MANAGE')")
    public ResponseEntity<StandardResponse> checkInChef(@PathVariable Long chefId) {
        kitchenChefService.checkInChef(chefId);
        return new ResponseEntity<>(new StandardResponse(200, "Chef checked in successfully", null), HttpStatus.OK);
    }

    @PostMapping("/chefs/{chefId}/check-out")
    @PreAuthorize("hasAuthority('KITCHEN_CHEF_MANAGE')")
    public ResponseEntity<StandardResponse> checkOutChef(@PathVariable Long chefId) {
        kitchenChefService.checkOutChef(chefId);
        return new ResponseEntity<>(new StandardResponse(200, "Chef checked out successfully", null), HttpStatus.OK);
    }

    @PutMapping("/chefs/{chefId}/work-status")
    @PreAuthorize("hasAuthority('KITCHEN_CHEF_MANAGE')")
    public ResponseEntity<StandardResponse> updateChefWorkStatus(
            @PathVariable Long chefId,
            @RequestBody UpdateWorkStatusDTO request) {
        kitchenChefService.updateChefWorkStatus(chefId, request.getNewStatus());
        return new ResponseEntity<>(
                new StandardResponse(200, "Chef status updated to " + request.getNewStatus(), null),
                HttpStatus.OK
        );
    }
}
