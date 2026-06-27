package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateAlertRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;
import com.ByteKnights.com.resturarent_system.service.KitchenAlertService;
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
public class KitchenAlertController {

    private final KitchenAlertService kitchenAlertService;

    @PostMapping("/alerts")
    @PreAuthorize("hasAuthority('KITCHEN_ALERT_CREATE')")
    public ResponseEntity<StandardResponse> createKitchenAlert(
            @Valid @RequestBody CreateAlertRequestDTO requestDTO,
            Principal principal) {
        kitchenAlertService.createKitchenAlert(requestDTO, principal.getName());
        return new ResponseEntity<>(
                new StandardResponse(201, "Alert broadcasted successfully!", null),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('KITCHEN_ALERT_VIEW')")
    public ResponseEntity<StandardResponse> getActiveAlerts(Principal principal) {
        List<ActiveAlertDTO> alerts = kitchenAlertService.getActiveAlerts(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", alerts), HttpStatus.OK);
    }

    @PutMapping("/alerts/{id}/resolve")
    @PreAuthorize("hasAuthority('KITCHEN_ALERT_RESOLVE')")
    public ResponseEntity<StandardResponse> resolveAlert(
            @PathVariable Long id,
            Principal principal) {
        kitchenAlertService.resolveAlert(id, principal.getName());
        return new ResponseEntity<>(
                new StandardResponse(200, "Alert marked as resolved", null),
                HttpStatus.OK
        );
    }
}
