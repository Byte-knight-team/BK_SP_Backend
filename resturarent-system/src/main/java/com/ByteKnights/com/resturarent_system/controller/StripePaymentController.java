package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentIntentRequest;
import com.ByteKnights.com.resturarent_system.service.StripePaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer/payments")
@CrossOrigin(origins = "*")
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;

    @Autowired
    public StripePaymentController(StripePaymentService stripePaymentService) {
        this.stripePaymentService = stripePaymentService;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<StandardResponse> createPaymentIntent(@Valid @RequestBody PaymentIntentRequest request) {
        String clientSecret = stripePaymentService.createPaymentIntent(
            request.getAmount(), 
            request.getOrderId(), 
            request.getReservationId()
        );

        Map<String, String> data = new HashMap<>();
        data.put("clientSecret", clientSecret);

        return new ResponseEntity<>(
                new StandardResponse(200, "Payment Intent created successfully", data),
                HttpStatus.OK
        );
    }
}
