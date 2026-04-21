package com.ByteKnights.com.resturarent_system.controller.kitchen;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.service.kitchen.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kitchen")
@CrossOrigin
@RequiredArgsConstructor
public class KitchenController {

    private final KitchenService kitchenService;

    @GetMapping("/stats")
    public ResponseEntity<StandardResponse> getKitchenDashboardStats() {
        KitchenDashboardStatsDTO stats = kitchenService.getKitchenDashboardStats();

        return new ResponseEntity<>(
                new StandardResponse(200, "Success", stats),
                HttpStatus.OK
        );
    }
}

