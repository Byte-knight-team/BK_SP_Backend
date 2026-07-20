package com.ByteKnights.com.resturarent_system.service;



public interface StripePaymentService {
    String createPaymentIntent(double amount, Long orderId, Long reservationId);
}
