package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerProfileResponse;
import com.ByteKnights.com.resturarent_system.service.CustomerProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        // The Principal object contains the email of the currently logged-in user 
        // (automatically injected by your Spring Security JWT filter)
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized access"));
        }

        String userEmail = principal.getName();
        CustomerProfileResponse profile = customerProfileService.getCustomerProfile(userEmail);
        
        return ResponseEntity.ok(Map.of("data", profile));
    }
}