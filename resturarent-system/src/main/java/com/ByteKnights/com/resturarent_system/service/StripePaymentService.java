package com.ByteKnights.com.resturarent_system.service;

import java.math.*;
import java.util.Map;

public interface StripePaymentService {
    String createPaymentIntent(BigDecimal amount, Long orderId, Long reservationId);

    boolean refundPayment(String paymentIntentId, BigDecimal amount, String idempotencyKey, String reason,
            Map<String, String> metadata);
}
