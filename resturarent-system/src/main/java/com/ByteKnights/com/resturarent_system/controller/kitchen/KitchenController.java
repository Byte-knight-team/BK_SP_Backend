package com.ByteKnights.com.resturarent_system.controller.kitchen;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PeakHourDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PopularMealDTO;
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
        List<PopularMealDTO> popularMeals = kitchenService.getMostPopularMeals();
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", popularMeals),
                HttpStatus.OK
        );
    }

    //get peak hours data
    @GetMapping("/peak-hours")
    public ResponseEntity<StandardResponse> getPeakHours() {
        List<PeakHourDTO> peakHours = kitchenService.getPeakHours();
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", peakHours),
                HttpStatus.OK
        );
    }


}


