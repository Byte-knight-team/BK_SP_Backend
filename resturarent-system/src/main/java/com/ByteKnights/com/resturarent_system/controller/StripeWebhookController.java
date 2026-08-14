package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.service.StripeWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    @Autowired
    public StripeWebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        stripeWebhookService.processWebhookEvent(payload, sigHeader);
        return ResponseEntity.ok("Success");
    }
}
