package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CheckoutCalculateRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CheckoutCalculateResponse;
import com.ByteKnights.com.resturarent_system.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/checkout")
@CrossOrigin
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<CheckoutCalculateResponse>> calculateTotals(
            Principal principal,
            @RequestBody CheckoutCalculateRequest request) {
        
        // QR customers or Online customers will have their identifier here
        String userIdentifier = principal != null ? principal.getName() : null;
        
        CheckoutCalculateResponse response = checkoutService.calculateOrderTotals(userIdentifier, request);
        
        return ResponseEntity.ok(ApiResponse.success("Checkout calculated successfully", response));
    }
}