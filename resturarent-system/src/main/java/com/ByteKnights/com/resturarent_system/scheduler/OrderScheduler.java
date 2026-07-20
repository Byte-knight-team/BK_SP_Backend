package com.ByteKnights.com.resturarent_system.scheduler;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.PaymentStatus;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Autowired
    public OrderScheduler(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /**
     * Runs every 5 minutes to clean up abandoned orders.
     * An order is abandoned if it was PLACED, payment is still PENDING (or FAILED),
     * and it was created more than 15 minutes ago.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void cleanupAbandonedOrders() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(15);
        log.info("Running Order Cleanup Scheduler. Checking for abandoned orders older than {}", thresholdTime);

        // Check for PENDING payments (only CARD)
        List<Order> abandonedPendingOrders = orderRepository.findByStatusAndPaymentStatusAndPaymentMethodAndCreatedAtBefore(
                OrderStatus.PLACED, PaymentStatus.PENDING, com.ByteKnights.com.resturarent_system.entity.PaymentMethod.CARD, thresholdTime
        );

        // Check for FAILED payments (if the webhook updated it but they never retried, only CARD)
        List<Order> abandonedFailedOrders = orderRepository.findByStatusAndPaymentStatusAndPaymentMethodAndCreatedAtBefore(
                OrderStatus.PLACED, PaymentStatus.FAILED, com.ByteKnights.com.resturarent_system.entity.PaymentMethod.CARD, thresholdTime
        );

        abandonedPendingOrders.addAll(abandonedFailedOrders);

        if (!abandonedPendingOrders.isEmpty()) {
            log.info("Found {} abandoned order(s). Processing cancellations...", abandonedPendingOrders.size());
            for (Order order : abandonedPendingOrders) {
                try {
                    orderService.expireAbandonedOrder(order.getId());
                    log.info("Successfully cancelled abandoned Order #{}", order.getOrderNumber());
                } catch (Exception e) {
                    log.error("Failed to cancel abandoned Order #{}: {}", order.getOrderNumber(), e.getMessage(), e);
                }
            }
        }
    }
}
