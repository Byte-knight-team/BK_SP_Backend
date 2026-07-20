package com.ByteKnights.com.resturarent_system.service;

public interface StripeWebhookService {
    void processWebhookEvent(String payload, String sigHeader);
}
