package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest;
import com.ByteKnights.com.resturarent_system.exception.PaymentGatewayException;
import com.ByteKnights.com.resturarent_system.service.CustomerReservationService;
import com.ByteKnights.com.resturarent_system.service.OrderService;
import com.ByteKnights.com.resturarent_system.service.StripeWebhookService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;

@Service
public class StripeWebhookServiceImpl implements StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookServiceImpl.class);

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final OrderService orderService;
    private final CustomerReservationService customerReservationService;
    private final PaymentRepository paymentRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Autowired
    public StripeWebhookServiceImpl(OrderService orderService, CustomerReservationService customerReservationService,
            PaymentRepository paymentRepository,
            ReservationPaymentRepository reservationPaymentRepository,
            OrderRepository orderRepository,
            ReservationRepository reservationRepository,
            WebSocketNotificationService webSocketNotificationService) {
        this.orderService = orderService;
        this.customerReservationService = customerReservationService;
        this.paymentRepository = paymentRepository;
        this.reservationPaymentRepository = reservationPaymentRepository;
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.webSocketNotificationService = webSocketNotificationService;
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
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(payload);
                JsonNode objectNode = root.path("data").path("object");

                String transactionId = objectNode.path("id").asText(null);
                JsonNode metadata = objectNode.path("metadata");

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
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(payload);
                JsonNode objectNode = root.path("data").path("object");

                String transactionId = objectNode.path("id").asText(null);
                JsonNode metadata = objectNode.path("metadata");

                if (transactionId != null && !metadata.isMissingNode()) {
                    String orderIdStr = metadata.path("orderId").asText(null);

                    if (orderIdStr != null && !orderIdStr.isBlank() && !"null".equals(orderIdStr)) {
                        try {
                            Long orderId = Long.parseLong(orderIdStr);
                            com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest request = new com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest();
                            request.setPaymentStatus("FAILED");
                            request.setTransactionId(transactionId);
                            orderService.updatePaymentStatus(orderId, request);
                            log.info("Marked Order #{} payment as FAILED. Waiting for retry or automated cleanup.",
                                    orderId);
                        } catch (Exception e) {
                            log.error("Failed to mark Order #{} as FAILED from Webhook.", orderIdStr, e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse failed Stripe webhook payload", e);
            }
        } else if ("charge.refunded".equals(event.getType())) {
            log.info("Stripe Charge Refunded Event: {}", event.getId());
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(payload);
                JsonNode objectNode = root.path("data").path("object");

                String paymentIntentId = objectNode.path("payment_intent").asText(null);

                if (paymentIntentId != null && !paymentIntentId.isBlank() && !"null".equals(paymentIntentId)) {
                    // Update Orders and Payments directly using JPQL to avoid Lost Update race
                    // conditions
                    int updatedPayments = paymentRepository.updatePaymentStatusByTransactionReference(paymentIntentId,
                            PaymentStatus.REFUNDED);
                    if (updatedPayments > 0) {
                        paymentRepository.updateOrderPaymentStatusByTxnRef(paymentIntentId, PaymentStatus.REFUNDED);
                        log.info("Successfully marked Payment and Order as REFUNDED via Webhook for txn: {}",
                                paymentIntentId);

                        // Broadcast WebSocket update for frontend
                        Long orderId = paymentRepository.findOrderIdByTransactionReference(paymentIntentId)
                                .orElse(null);
                        if (orderId != null) {
                            webSocketNotificationService.broadcastOrderPaymentStatusUpdate(orderId,
                                    PaymentStatus.REFUNDED.name());
                        }
                    }

                    // Try to find in Reservations
                    int updatedResPayments = reservationPaymentRepository
                            .updatePaymentStatusByTransactionReference(paymentIntentId, PaymentStatus.REFUNDED);
                    if (updatedResPayments > 0) {
                        log.info("Successfully marked Reservation Payment as REFUNDED via Webhook for txn: {}",
                                paymentIntentId);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse charge.refunded webhook payload", e);
            }
        } else {
            log.debug("Unhandled Stripe webhook event type: {}", event.getType());
        }
    }
}
