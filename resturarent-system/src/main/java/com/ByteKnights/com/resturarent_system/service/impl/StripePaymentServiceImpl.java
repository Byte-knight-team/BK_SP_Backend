package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.exception.PaymentGatewayException;
import com.ByteKnights.com.resturarent_system.service.StripePaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentServiceImpl implements StripePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentServiceImpl.class);

    @Override
    public String createPaymentIntent(double amount, Long orderId, Long reservationId) {
        try {
            // Stripe expects the amount in the smallest currency unit.
            // For LKR, we multiply by 100.
            long amountInCents = Math.round(amount * 100);

            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                            .setAmount(amountInCents)
                            .setCurrency("lkr")
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .build()
                            );
            
            if (orderId != null) {
                builder.putMetadata("orderId", String.valueOf(orderId));
            }
            if (reservationId != null) {
                builder.putMetadata("reservationId", String.valueOf(reservationId));
            }

            PaymentIntent paymentIntent = PaymentIntent.create(builder.build());
            return paymentIntent.getClientSecret();
            
        } catch (StripeException e) {
            log.error("Stripe payment intent creation failed: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to initialize secure payment. Please try again later.");
        } catch (Exception e) {
            log.error("Unexpected error during payment intent creation", e);
            throw new PaymentGatewayException("An unexpected error occurred processing your payment request.");
        }
    }
}
