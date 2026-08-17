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

/**
 * Implementation of {@link StripePaymentService} for direct Stripe API interactions.
 * 
 * <p>Key Responsibilities:
 * <ul>
 *   <li><b>Payment Intent Generation:</b> Creates secure Stripe {@link PaymentIntent} objects
 *       with embedded metadata (orderId, reservationId) and returns the client secret for frontend checkout.</li>
 *   <li><b>Refund Processing:</b> Calls Stripe's {@link Refund} API to issue full or partial refunds
 *       with idempotency key protection to prevent duplicate refund charges on network retries.</li>
 *   <li><b>Currency Normalization:</b> Converts standard currency amounts (LKR) to the smallest currency unit (cents).</li>
 * </ul>
 */
@Service
public class StripePaymentServiceImpl implements StripePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentServiceImpl.class);

    /**
     * Creates a Stripe {@link PaymentIntent} and returns the {@code clientSecret} required
     * by the frontend Stripe Elements UI to finalize payment.
     *
     * @param amount        The monetary amount to charge in standard currency units (LKR).
     * @param orderId       Optional Order ID attached as metadata for webhook reconciliation.
     * @param reservationId Optional Reservation ID attached as metadata for webhook reconciliation.
     * @return The client secret string used by Stripe Elements on the client side.
     * @throws PaymentGatewayException If Stripe rejects the intent creation or communication fails.
     */
    @Override
    public String createPaymentIntent(BigDecimal amount, Long orderId, Long reservationId) {
        try {
            // Stripe expects the amount in the smallest currency unit.
            // For LKR, 1 LKR = 100 cents.
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                            .setAmount(amountInCents)
                            .setCurrency("lkr")
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .build()
                            );
            
            // Attach domain IDs to Stripe metadata so the webhook can identify the resource
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

    /**
     * Executes a full or partial refund against an existing Stripe {@link PaymentIntent}.
     *
     * <p>Includes an <b>Idempotency Key</b> to safeguard against duplicate refunds in case of
     * network drops or automatic HTTP retries.
     *
     * @param paymentIntentId The Stripe PaymentIntent ID (e.g., "pi_3M...").
     * @param amount          The amount to refund in LKR (if {@code null}, Stripe issues a 100% full refund).
     * @param idempotencyKey  Unique key (e.g., "order-cancel-123") ensuring this refund executes only once.
     * @param reason          Reason string (e.g., "requested_by_customer", "duplicate", "fraudulent").
     * @param metadata        Custom key-value metadata to attach to the Stripe Refund object.
     * @return {@code true} if the refund succeeded with Stripe, {@code false} otherwise.
     */
    @Override
    public boolean refundPayment(String paymentIntentId, BigDecimal amount, String idempotencyKey, String reason, Map<String, String> metadata) {
        try {
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId);

            // Partial refund if amount is specified; full refund if amount is null
            if (amount != null) {
                long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
                builder.setAmount(amountInCents);
            }

            // Map reason to Stripe's standardized Reason enum
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

            // Pass IdempotencyKey via RequestOptions to prevent duplicate refunds on retries
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
