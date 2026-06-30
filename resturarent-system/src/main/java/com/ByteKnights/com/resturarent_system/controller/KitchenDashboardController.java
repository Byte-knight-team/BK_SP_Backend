package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PeakHourDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PopularMealDTO;
import com.ByteKnights.com.resturarent_system.service.KitchenDashboardService;
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
public class KitchenDashboardController {

    private final KitchenDashboardService kitchenDashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('KITCHEN_VIEW_STATS')")
    public ResponseEntity<StandardResponse> getKitchenDashboardStats(Principal principal) {
        KitchenDashboardStatsDTO stats = kitchenDashboardService.getKitchenDashboardStats(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", stats), HttpStatus.OK);
    }

    @GetMapping("/popular-meals")
    @PreAuthorize("hasAuthority('KITCHEN_VIEW_STATS')")
    public ResponseEntity<StandardResponse> getMostPopularMeals(Principal principal) {
        List<PopularMealDTO> popularMeals = kitchenDashboardService.getMostPopularMealsInLast7Days(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", popularMeals), HttpStatus.OK);
    }

    @GetMapping("/peak-hours")
    @PreAuthorize("hasAuthority('KITCHEN_VIEW_STATS')")
    public ResponseEntity<StandardResponse> getPeakHours(Principal principal) {
        List<PeakHourDTO> peakHours = kitchenDashboardService.getPeakHoursInLast7Days(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", peakHours), HttpStatus.OK);
    }
}
