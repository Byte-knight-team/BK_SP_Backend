package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest;
import com.ByteKnights.com.resturarent_system.exception.PaymentGatewayException;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.CustomerReservationService;
import com.ByteKnights.com.resturarent_system.service.OrderService;
import com.ByteKnights.com.resturarent_system.service.StripeWebhookService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of {@link StripeWebhookService} that processes asynchronous webhook events from Stripe.
 *
 * <p>Key Architectural Design Principles:
 * <ul>
 *   <li><b>Cryptographic Signature Verification:</b> Every incoming payload is validated against the
 *       {@code Stripe-Signature} header using HMAC-SHA256 and the configured webhook signing secret
 *       before any processing occurs.</li>
 *   <li><b>Version-Agnostic JSON Parsing:</b> Uses Spring's Jackson {@link ObjectMapper} directly on the
 *       raw JSON string instead of Stripe's SDK deserializer. This prevents runtime
 *       {@code EventDataObjectDeserializationException} if Stripe's account API version is newer than
 *       the SDK version in {@code pom.xml}.</li>
 *   <li><b>Atomic Database Updates & Audit Logging:</b> Updates payment and order states within a database
 *       transaction and records comprehensive audit snapshots via {@link AuditLogService}.</li>
 *   <li><b>Real-Time UI Synchronization:</b> Broadcasts STOMP WebSocket messages immediately upon status changes
 *       so connected frontend clients update their UI without polling.</li>
 * </ul>
 */
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
    private final AuditLogService auditLogService;

    @Autowired
    public StripeWebhookServiceImpl(OrderService orderService,
                                    CustomerReservationService customerReservationService,
                                    PaymentRepository paymentRepository,
                                    ReservationPaymentRepository reservationPaymentRepository,
                                    OrderRepository orderRepository,
                                    ReservationRepository reservationRepository,
                                    WebSocketNotificationService webSocketNotificationService,
                                    AuditLogService auditLogService) {
        this.orderService = orderService;
        this.customerReservationService = customerReservationService;
        this.paymentRepository = paymentRepository;
        this.reservationPaymentRepository = reservationPaymentRepository;
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.webSocketNotificationService = webSocketNotificationService;
        this.auditLogService = auditLogService;
    }

    /**
     * Primary entry point for processing incoming raw Stripe webhook payloads.
     *
     * @param payload   The raw HTTP request body string from Stripe.
     * @param sigHeader The {@code Stripe-Signature} header containing the timestamp and HMAC signature.
     * @throws PaymentGatewayException If signature verification fails or payload is malformed.
     */
    @Override
    @Transactional
    public void processWebhookEvent(String payload, String sigHeader) {
        if (endpointSecret == null || endpointSecret.isBlank()) {
            log.error("Stripe Webhook Secret is not configured.");
            throw new PaymentGatewayException("Webhook secret not configured");
        }

        Event event;

        // 1. Cryptographically verify that the payload was generated by Stripe
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed.");
            throw new PaymentGatewayException("Invalid signature");
        } catch (Exception e) {
            log.error("Webhook payload parsing failed.", e);
            throw new PaymentGatewayException("Invalid payload");
        }

        // 2. Route event to corresponding domain handler based on Stripe event type
        if ("payment_intent.succeeded".equals(event.getType())) {
            handlePaymentIntentSucceeded(payload, event.getId());
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            handlePaymentIntentFailed(payload, event.getId());
        } else if ("charge.refunded".equals(event.getType())) {
            handleChargeRefunded(payload, event.getId());
        } else {
            log.debug("Unhandled Stripe webhook event type: {}", event.getType());
        }
    }

    /**
     * Handles successful payment intents ({@code payment_intent.succeeded}).
     * Parses metadata to determine if the payment belongs to an Order or a Reservation.
     */
    private void handlePaymentIntentSucceeded(String payload, String stripeEventId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);
            JsonNode objectNode = root.path("data").path("object");

            String transactionId = objectNode.path("id").asText(null);
            JsonNode metadata = objectNode.path("metadata");

            if (transactionId != null && !metadata.isMissingNode()) {
                String orderIdStr = metadata.path("orderId").asText(null);
                String reservationIdStr = metadata.path("reservationId").asText(null);

                if (orderIdStr != null && !orderIdStr.isBlank() && !"null".equals(orderIdStr)) {
                    handleSuccessfulOrderPayment(orderIdStr, transactionId, stripeEventId);
                } else if (reservationIdStr != null && !reservationIdStr.isBlank()
                        && !"null".equals(reservationIdStr)) {
                    handleSuccessfulReservationPayment(reservationIdStr, transactionId, stripeEventId);
                } else {
                    log.warn(
                            "PaymentIntent {} succeeded but contained no identifiable orderId or reservationId metadata.",
                            transactionId
                    );
                }
            } else {
                log.warn("Could not extract transaction ID or metadata from raw payload.");
            }
        } catch (Exception e) {
            log.error("Failed to parse raw Stripe webhook payload", e);
        }
    }

    /**
     * Updates an Order's payment status to {@link PaymentStatus#PAID}, persists audit logs,
     * and broadcasts real-time WebSocket notifications.
     */
    private void handleSuccessfulOrderPayment(String orderIdStr, String transactionId, String stripeEventId) {
        try {
            Long orderId = Long.parseLong(orderIdStr);

            // Capture pre-update state for audit trail
            Order oldOrder = orderRepository.findById(orderId).orElse(null);
            Map<String, Object> oldValues = buildOrderPaymentAuditSnapshot(
                    oldOrder,
                    transactionId,
                    stripeEventId,
                    "payment_intent.succeeded"
            );

            // Delegate to order service for domain transitions, loyalty updates, and receipts
            PaymentUpdateRequest request = new PaymentUpdateRequest();
            request.setPaymentStatus("PAID");
            request.setTransactionId(transactionId);

            orderService.updatePaymentStatus(orderId, request);

            // Capture post-update state for audit trail
            Order updatedOrder = orderRepository.findById(orderId).orElse(null);
            Map<String, Object> newValues = buildOrderPaymentAuditSnapshot(
                    updatedOrder,
                    transactionId,
                    stripeEventId,
                    "payment_intent.succeeded"
            );

            auditLogService.logCurrentUserAction(
                    AuditModule.PAYMENT,
                    AuditEventType.PAYMENT_STATUS_UPDATED,
                    AuditStatus.SUCCESS,
                    AuditSeverity.INFO,
                    AuditTargetType.ORDER,
                    orderId,
                    getOrderBranchId(updatedOrder != null ? updatedOrder : oldOrder),
                    "Order payment marked as PAID by Stripe webhook",
                    oldValues,
                    newValues
            );

            log.info("Successfully updated Order #{} to PAID via Webhook.", orderId);
        } catch (Exception e) {
            log.error("Failed to update Order #{} from Webhook.", orderIdStr, e);
        }
    }

    /**
     * Finalizes table reservation payments and records audit entries upon payment success.
     */
    private void handleSuccessfulReservationPayment(String reservationIdStr, String transactionId, String stripeEventId) {
        try {
            Long reservationId = Long.parseLong(reservationIdStr);

            Map<String, Object> oldValues = buildReservationPaymentAuditSnapshot(
                    reservationId,
                    null,
                    transactionId,
                    stripeEventId,
                    "payment_intent.succeeded"
            );

            customerReservationService.webhookPayReservation(reservationId, transactionId);

            Map<String, Object> newValues = buildReservationPaymentAuditSnapshot(
                    reservationId,
                    PaymentStatus.PAID,
                    transactionId,
                    stripeEventId,
                    "payment_intent.succeeded"
            );

            auditLogService.logCurrentUserAction(
                    AuditModule.PAYMENT,
                    AuditEventType.PAYMENT_STATUS_UPDATED,
                    AuditStatus.SUCCESS,
                    AuditSeverity.INFO,
                    AuditTargetType.PAYMENT,
                    reservationId,
                    null,
                    "Reservation payment marked as PAID by Stripe webhook",
                    oldValues,
                    newValues
            );

            log.info("Successfully updated Reservation #{} to PAID via Webhook.", reservationIdStr);
        } catch (Exception e) {
            log.error("Failed to update Reservation #{} from Webhook.", reservationIdStr, e);
        }
    }

    /**
     * Handles payment failure events ({@code payment_intent.payment_failed}).
     */
    private void handlePaymentIntentFailed(String payload, String stripeEventId) {
        log.warn("Stripe Payment Failed for Event ID: {}", stripeEventId);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);
            JsonNode objectNode = root.path("data").path("object");

            String transactionId = objectNode.path("id").asText(null);
            JsonNode metadata = objectNode.path("metadata");

            if (transactionId != null && !metadata.isMissingNode()) {
                String orderIdStr = metadata.path("orderId").asText(null);

                if (orderIdStr != null && !orderIdStr.isBlank() && !"null".equals(orderIdStr)) {
                    handleFailedOrderPayment(orderIdStr, transactionId, stripeEventId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse failed Stripe webhook payload", e);
        }
    }

    /**
     * Updates an Order's payment status to {@link PaymentStatus#FAILED}, allowing the customer
     * a 15-minute retry window before the scheduled cleaner expires the order.
     */
    private void handleFailedOrderPayment(String orderIdStr, String transactionId, String stripeEventId) {
        try {
            Long orderId = Long.parseLong(orderIdStr);

            Order oldOrder = orderRepository.findById(orderId).orElse(null);
            Map<String, Object> oldValues = buildOrderPaymentAuditSnapshot(
                    oldOrder,
                    transactionId,
                    stripeEventId,
                    "payment_intent.payment_failed"
            );

            PaymentUpdateRequest request = new PaymentUpdateRequest();
            request.setPaymentStatus("FAILED");
            request.setTransactionId(transactionId);

            orderService.updatePaymentStatus(orderId, request);

            Order updatedOrder = orderRepository.findById(orderId).orElse(null);
            Map<String, Object> newValues = buildOrderPaymentAuditSnapshot(
                    updatedOrder,
                    transactionId,
                    stripeEventId,
                    "payment_intent.payment_failed"
            );

            auditLogService.logCurrentUserAction(
                    AuditModule.PAYMENT,
                    AuditEventType.PAYMENT_STATUS_UPDATED,
                    AuditStatus.SUCCESS,
                    AuditSeverity.WARN,
                    AuditTargetType.ORDER,
                    orderId,
                    getOrderBranchId(updatedOrder != null ? updatedOrder : oldOrder),
                    "Order payment marked as FAILED by Stripe webhook",
                    oldValues,
                    newValues
            );

            log.info("Marked Order #{} payment as FAILED. Waiting for retry or automated cleanup.", orderId);
        } catch (Exception e) {
            log.error("Failed to mark Order #{} as FAILED from Webhook.", orderIdStr, e);
        }
    }

    /**
     * Handles Stripe charge refund events ({@code charge.refunded}).
     * Reconciles both Order and Reservation payment records associated with the Stripe PaymentIntent.
     */
    private void handleChargeRefunded(String payload, String stripeEventId) {
        log.info("Stripe Charge Refunded Event: {}", stripeEventId);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);
            JsonNode objectNode = root.path("data").path("object");

            String paymentIntentId = objectNode.path("payment_intent").asText(null);

            if (paymentIntentId != null && !paymentIntentId.isBlank() && !"null".equals(paymentIntentId)) {
                handleOrderRefund(paymentIntentId, stripeEventId);
                handleReservationRefund(paymentIntentId, stripeEventId);
            }
        } catch (Exception e) {
            log.error("Failed to parse charge.refunded webhook payload", e);
        }
    }

    /**
     * Updates an Order and its Payment record to {@link PaymentStatus#REFUNDED} using atomic JPQL queries,
     * writes an audit log entry, and broadcasts a real-time WebSocket update to the customer.
     */
    private void handleOrderRefund(String paymentIntentId, String stripeEventId) {
        Long orderId = paymentRepository.findOrderIdByTransactionReference(paymentIntentId).orElse(null);
        Order oldOrder = orderId != null ? orderRepository.findById(orderId).orElse(null) : null;

        Map<String, Object> oldValues = buildOrderPaymentAuditSnapshot(
                oldOrder,
                paymentIntentId,
                stripeEventId,
                "charge.refunded"
        );

        // Atomic JPQL updates prevent race conditions and lost update anomalies
        int updatedPayments = paymentRepository.updatePaymentStatusByTransactionReference(
                paymentIntentId,
                PaymentStatus.REFUNDED
        );

        if (updatedPayments > 0) {
            paymentRepository.updateOrderPaymentStatusByTxnRef(paymentIntentId, PaymentStatus.REFUNDED);

            Order updatedOrder = orderId != null ? orderRepository.findById(orderId).orElse(null) : null;

            Map<String, Object> newValues = buildOrderPaymentAuditSnapshot(
                    updatedOrder,
                    paymentIntentId,
                    stripeEventId,
                    "charge.refunded"
            );

            auditLogService.logCurrentUserAction(
                    AuditModule.PAYMENT,
                    AuditEventType.PAYMENT_STATUS_UPDATED,
                    AuditStatus.SUCCESS,
                    AuditSeverity.WARN,
                    AuditTargetType.ORDER,
                    orderId,
                    getOrderBranchId(updatedOrder != null ? updatedOrder : oldOrder),
                    "Order payment marked as REFUNDED by Stripe webhook",
                    oldValues,
                    newValues
            );

            log.info("Successfully marked Payment and Order as REFUNDED via Webhook for txn: {}", paymentIntentId);

            // Broadcast real-time status update to frontend clients
            if (orderId != null) {
                webSocketNotificationService.broadcastOrderPaymentStatusUpdate(
                        orderId,
                        PaymentStatus.REFUNDED.name()
                );
            }
        }
    }

    /**
     * Updates a Reservation's payment record to {@link PaymentStatus#REFUNDED} and records an audit log.
     */
    private void handleReservationRefund(String paymentIntentId, String stripeEventId) {
        Map<String, Object> oldValues = buildReservationPaymentAuditSnapshot(
                null,
                null,
                paymentIntentId,
                stripeEventId,
                "charge.refunded"
        );

        int updatedResPayments = reservationPaymentRepository.updatePaymentStatusByTransactionReference(
                paymentIntentId,
                PaymentStatus.REFUNDED
        );

        if (updatedResPayments > 0) {
            Map<String, Object> newValues = buildReservationPaymentAuditSnapshot(
                    null,
                    PaymentStatus.REFUNDED,
                    paymentIntentId,
                    stripeEventId,
                    "charge.refunded"
            );

            auditLogService.logCurrentUserAction(
                    AuditModule.PAYMENT,
                    AuditEventType.PAYMENT_STATUS_UPDATED,
                    AuditStatus.SUCCESS,
                    AuditSeverity.WARN,
                    AuditTargetType.PAYMENT,
                    null,
                    null,
                    "Reservation payment marked as REFUNDED by Stripe webhook",
                    oldValues,
                    newValues
            );

            log.info("Successfully marked Reservation Payment as REFUNDED via Webhook for txn: {}", paymentIntentId);
        }
    }

    /**
     * Constructs a structured audit snapshot map for Order payment events.
     */
    private Map<String, Object> buildOrderPaymentAuditSnapshot(Order order,
                                                               String transactionId,
                                                               String stripeEventId,
                                                               String stripeEventType) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("stripeEventId", stripeEventId);
        snapshot.put("stripeEventType", stripeEventType);
        snapshot.put("transactionId", transactionId);

        if (order == null) {
            return snapshot;
        }

        snapshot.put("orderId", order.getId());
        snapshot.put("orderNumber", order.getOrderNumber());
        snapshot.put("orderStatus", order.getStatus() != null ? order.getStatus().name() : null);
        snapshot.put("orderType", order.getOrderType() != null ? order.getOrderType().name() : null);
        snapshot.put("paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        snapshot.put("branchId", getOrderBranchId(order));

        return snapshot;
    }

    /**
     * Constructs a structured audit snapshot map for Reservation payment events.
     */
    private Map<String, Object> buildReservationPaymentAuditSnapshot(Long reservationId,
                                                                     PaymentStatus paymentStatus,
                                                                     String transactionId,
                                                                     String stripeEventId,
                                                                     String stripeEventType) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("reservationId", reservationId);
        snapshot.put("paymentStatus", paymentStatus != null ? paymentStatus.name() : null);
        snapshot.put("transactionId", transactionId);
        snapshot.put("stripeEventId", stripeEventId);
        snapshot.put("stripeEventType", stripeEventType);

        return snapshot;
    }

    /**
     * Safely retrieves the branch ID associated with an Order.
     */
    private Long getOrderBranchId(Order order) {
        if (order == null || order.getBranch() == null) {
            return null;
        }

        return order.getBranch().getId();
    }
}