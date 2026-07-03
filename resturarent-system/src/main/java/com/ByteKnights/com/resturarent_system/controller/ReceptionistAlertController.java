package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;
import com.ByteKnights.com.resturarent_system.service.ReceptionistAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/receptionist/alerts")
@CrossOrigin
@RequiredArgsConstructor
public class ReceptionistAlertController {

    private final ReceptionistAlertService receptionistAlertService;

    @GetMapping
    @PreAuthorize("hasAuthority('KITCHEN_ALERT_VIEW')")
    public ResponseEntity<StandardResponse> getAlerts(Principal principal) {
        List<ActiveAlertDTO> alerts = receptionistAlertService.getAlerts(principal.getName());
        return ResponseEntity.ok(new StandardResponse(200, "Kitchen alerts fetched", alerts));
    }
}
