package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.service.email.EmailTestingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email-testing")
public class EmailTestingController {

    private final EmailTestingService emailTestingService;

    public EmailTestingController(EmailTestingService emailTestingService) {
        this.emailTestingService = emailTestingService;
    }

    // View current force-fail status
    @GetMapping("/force-fail")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Boolean>> getForceFailStatus() {
        return ResponseEntity.ok(Map.of("forceFail", emailTestingService.isForceFail()));
    }

    // Turn force-fail on/off at runtime
    @PostMapping("/force-fail")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> setForceFailStatus(@RequestBody Map<String, Boolean> payload) {
        Boolean forceFail = payload.get("forceFail");

        if (forceFail == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "forceFail field is required"
            ));
        }

        emailTestingService.setForceFail(forceFail);

        return ResponseEntity.ok(Map.of(
                "message", "Email force-fail updated successfully",
                "forceFail", emailTestingService.isForceFail()
        ));
    }
}