package com.ByteKnights.com.resturarent_system.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank() || stripeSecretKey.contains("test_secret_key_here")) {
            log.warn("Stripe Secret Key is missing or using default placeholder. Payments may fail.");
        } else {
            Stripe.apiKey = stripeSecretKey;
            log.info("Stripe Java SDK initialized successfully.");
        }
    }
}
