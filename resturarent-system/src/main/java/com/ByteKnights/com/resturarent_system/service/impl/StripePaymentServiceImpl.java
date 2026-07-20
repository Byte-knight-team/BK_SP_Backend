package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.exception.PaymentGatewayException;
import com.ByteKnights.com.resturarent_system.service.StripePaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import com.stripe.net.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class StripePaymentServiceImpl implements StripePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentServiceImpl.class);

    @Override
    public String createPaymentIntent(BigDecimal amount, Long orderId, Long reservationId) {
        try {
            // Stripe expects the amount in the smallest currency unit.
            // For LKR, we multiply by 100.
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

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

    @Override
    public boolean refundPayment(String paymentIntentId, BigDecimal amount, String idempotencyKey, String reason, Map<String, String> metadata) {
        try {
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId);

            if (amount != null) {
                long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
                builder.setAmount(amountInCents);
            }

            if (reason != null && !reason.isBlank()) {
                if ("requested_by_customer".equalsIgnoreCase(reason)) {
                    builder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
                } else if ("duplicate".equalsIgnoreCase(reason)) {
                    builder.setReason(RefundCreateParams.Reason.DUPLICATE);
                } else if ("fraudulent".equalsIgnoreCase(reason)) {
                    builder.setReason(RefundCreateParams.Reason.FRAUDULENT);
                }
            }

            if (metadata != null && !metadata.isEmpty()) {
                builder.putAllMetadata(metadata);
            }

            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            Refund refund = Refund.create(builder.build(), requestOptions);
            log.info("Stripe refund created successfully: {}", refund.getId());
            return true;

        } catch (StripeException e) {
            log.error("Stripe refund failed for PaymentIntent {}: {}", paymentIntentId, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during Stripe refund creation for PaymentIntent {}", paymentIntentId, e);
            return false;
        }
    }
}
