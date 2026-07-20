package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest;
import com.ByteKnights.com.resturarent_system.exception.PaymentGatewayException;
import com.ByteKnights.com.resturarent_system.service.CustomerReservationService;
import com.ByteKnights.com.resturarent_system.service.OrderService;
import com.ByteKnights.com.resturarent_system.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookServiceImpl implements StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookServiceImpl.class);

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final OrderService orderService;
    private final CustomerReservationService customerReservationService;

    @Autowired
    public StripeWebhookServiceImpl(OrderService orderService, CustomerReservationService customerReservationService) {
        this.orderService = orderService;
        this.customerReservationService = customerReservationService;
    }

    @Override
    public void processWebhookEvent(String payload, String sigHeader) {
        if (endpointSecret == null || endpointSecret.isBlank()) {
            log.error("Stripe Webhook Secret is not configured.");
            throw new PaymentGatewayException("Webhook secret not configured");
        }

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed.");
            throw new PaymentGatewayException("Invalid signature");
        } catch (Exception e) {
            log.error("Webhook payload parsing failed.", e);
            throw new PaymentGatewayException("Invalid payload");
        }

        // Handle the event
        if ("payment_intent.succeeded".equals(event.getType())) {
            try {
                // IMPORTANT: We use Jackson ObjectMapper here instead of Stripe's
                // EventDataObjectDeserializer.
                // Stripe's Java SDK will throw an EventDataObjectDeserializationException if
                // the incoming
                // webhook payload is from a newer API version than the SDK version in pom.xml.
                // Parsing the raw JSON directly ensures our webhook remains robust and
                // version-agnostic.
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(payload);
                com.fasterxml.jackson.databind.JsonNode objectNode = root.path("data").path("object");

                String transactionId = objectNode.path("id").asText(null);
                com.fasterxml.jackson.databind.JsonNode metadata = objectNode.path("metadata");

                if (transactionId != null && !metadata.isMissingNode()) {
                    String orderIdStr = metadata.path("orderId").asText(null);
                    String reservationIdStr = metadata.path("reservationId").asText(null);

                    if (orderIdStr != null && !orderIdStr.isBlank() && !"null".equals(orderIdStr)) {
                        try {
                            Long orderId = Long.parseLong(orderIdStr);
                            PaymentUpdateRequest request = new PaymentUpdateRequest();
                            request.setPaymentStatus("PAID");
                            request.setTransactionId(transactionId);
                            orderService.updatePaymentStatus(orderId, request);
                            log.info("Successfully updated Order #{} to PAID via Webhook.", orderId);
                        } catch (Exception e) {
                            log.error("Failed to update Order #{} from Webhook.", orderIdStr, e);
                        }
                    } else if (reservationIdStr != null && !reservationIdStr.isBlank()
                            && !"null".equals(reservationIdStr)) {
                        try {
                            Long reservationId = Long.parseLong(reservationIdStr);
                            customerReservationService.webhookPayReservation(reservationId, transactionId);
                            log.info("Successfully updated Reservation #{} to PAID via Webhook.", reservationIdStr);
                        } catch (Exception e) {
                            log.error("Failed to update Reservation #{} from Webhook.", reservationIdStr, e);
                        }
                    } else {
                        log.warn(
                                "PaymentIntent {} succeeded but contained no identifiable orderId or reservationId metadata.",
                                transactionId);
                    }
                } else {
                    log.warn("Could not extract transaction ID or metadata from raw payload.");
                }
            } catch (Exception e) {
                log.error("Failed to parse raw Stripe webhook payload", e);
            }
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            log.warn("Stripe Payment Failed for Event ID: {}", event.getId());
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(payload);
                com.fasterxml.jackson.databind.JsonNode objectNode = root.path("data").path("object");
                
                String transactionId = objectNode.path("id").asText(null);
                com.fasterxml.jackson.databind.JsonNode metadata = objectNode.path("metadata");
                
                if (transactionId != null && !metadata.isMissingNode()) {
                    String orderIdStr = metadata.path("orderId").asText(null);
                    
                    if (orderIdStr != null && !orderIdStr.isBlank() && !"null".equals(orderIdStr)) {
                        try {
                            Long orderId = Long.parseLong(orderIdStr);
                            com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest request = new com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest();
                            request.setPaymentStatus("FAILED");
                            request.setTransactionId(transactionId);
                            orderService.updatePaymentStatus(orderId, request);
                            log.info("Marked Order #{} payment as FAILED. Waiting for retry or automated cleanup.", orderId);
                        } catch (Exception e) {
                            log.error("Failed to mark Order #{} as FAILED from Webhook.", orderIdStr, e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse failed Stripe webhook payload", e);
            }
        } else {
            log.debug("Unhandled Stripe webhook event type: {}", event.getType());
        }
    }
}
