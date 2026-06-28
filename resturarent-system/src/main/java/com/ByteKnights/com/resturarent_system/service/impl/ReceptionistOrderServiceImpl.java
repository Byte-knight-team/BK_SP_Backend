package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.*;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.ReceptionistOrderService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionistOrderServiceImpl implements ReceptionistOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final MenuItemIngredientRepository menuItemIngredientRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponRepository couponRepository;


    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── helper: resolve branch from logged-in receptionist ──────────────
    private Long getBranchId(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        return staff.getBranch().getId();
    }

    // ── GET orders by status (today, QR + ONLINE_PICKUP only) ───────────
    @Override
    public List<ReceptionistOrderSummaryDTO> getOrdersByStatus(String status, String userEmail) {
        Long branchId = getBranchId(userEmail);
        OrderStatus orderStatus = OrderStatus.valueOf(status);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(23, 59, 59);

        List<Order> orders = orderRepository
                .findByBranchIdAndStatusAndOrderTypeInAndCreatedAtBetween(
                        branchId,
                        orderStatus,
                        List.of(OrderType.QR, OrderType.ONLINE_PICKUP, OrderType.ONLINE_DELIVERY),
                        startOfDay,
                        endOfDay);

        List<ReceptionistOrderSummaryDTO> result = new ArrayList<>();
        for (Order order : orders) {
            String customerName = (order.getContactName() != null && !order.getContactName().isBlank())
                    ? order.getContactName()
                    : (order.getCustomer().getUser().getFullName() != null
                       ? order.getCustomer().getUser().getFullName() : "Guest");

            String customerPhone = (order.getContactPhone() != null && !order.getContactPhone().isBlank())
                    ? order.getContactPhone()
                    : order.getCustomer().getUser().getPhone();

            Integer tableNumber = (order.getTable() != null) ? order.getTable().getTableNumber() : null;

            int totalItems = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();

            double finalAmount = order.getFinalAmount() != null
                    ? order.getFinalAmount().doubleValue()
                    : order.getTotalAmount().doubleValue();

            result.add(new ReceptionistOrderSummaryDTO(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getOrderType().name(),
                    order.getStatus().name(),
                    order.getPaymentStatus().name(),
                    order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : "",
                    order.getStatusUpdatedAt() != null ? order.getStatusUpdatedAt().format(FORMATTER) : "",
                    customerName,
                    customerPhone,
                    tableNumber,
                    totalItems,
                    finalAmount
            ));
        }
        return result;
    }

    // ── GET full order detail ────────────────────────────────────────────
    @Override
    public ReceptionistOrderDetailDTO getOrderDetail(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String customerName = order.getCustomer().getUser().getFullName();
        String customerPhone = order.getCustomer().getUser().getPhone();
        String customerEmail = order.getCustomer().getUser().getEmail();

        Integer tableNumber = (order.getTable() != null) ? order.getTable().getTableNumber() : null;

        List<ReceptionistOrderItemDTO> items = order.getItems().stream()
                .map(item -> new ReceptionistOrderItemDTO(
                        item.getId(),
                        item.getItemName(),
                        item.getQuantity(),
                        item.getUnitPrice().doubleValue(),
                        item.getSubtotal() != null ? item.getSubtotal().doubleValue()
                                : item.getUnitPrice().doubleValue() * item.getQuantity(),
                        item.getStatus().name()
                )).toList();

        double finalAmount = order.getFinalAmount() != null
                ? order.getFinalAmount().doubleValue()
                : order.getTotalAmount().doubleValue();

        return new ReceptionistOrderDetailDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderType().name(),
                order.getStatus().name(),
                order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : "",
                customerName, customerPhone, customerEmail,
                order.getContactName(), order.getContactPhone(), order.getContactEmail(),
                tableNumber,
                items,
                order.getPaymentStatus().name(),
                order.getTotalAmount().doubleValue(),
                order.getTaxAmount() != null ? order.getTaxAmount().doubleValue() : 0,
                order.getServiceCharge() != null ? order.getServiceCharge().doubleValue() : 0,
                order.getDiscountAmount() != null ? order.getDiscountAmount().doubleValue() : 0,
                order.getAppliedCouponCode(),
                finalAmount,
                order.getKitchenNotes(),
                order.getHoldReason(),
                order.getCancelReason()
        );
    }

    // ── SEND TO KITCHEN: PLACED → PENDING ───────────────────────────────
    @Override
    @Transactional
    public void sendToKitchen(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new RuntimeException("Order cannot be sent to kitchen from current status");
        }

        order.updateStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        Long branchId = order.getBranch().getId();

        // Notify kitchen: refresh pending list + toast
        webSocketNotificationService.broadcastNewKitchenOrder(branchId, order.getOrderNumber());
    }

    // ── HOLD ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void holdOrder(Long orderId, String reason, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.ON_HOLD);
        order.setHoldReason(reason);
        order.setStatusUpdatedAt(java.time.LocalDateTime.now());
        orderRepository.save(order);
    }

    // ── CANCEL (with loyalty rollback + coupon restock, same as customer cancel) ──
    @Override
    @Transactional
    public void cancelOrder(Long orderId, String reason, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == OrderStatus.PREPARING
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException(
                    "Cannot cancel an order that is already preparing, completed, or cancelled.");
        }

        Customer customer = order.getCustomer();

        // Rollback loyalty points if not already refunded
        if (!loyaltyTransactionRepository.existsByOrderAndTransactionType(order, LoyaltyTransactionType.REFUND)) {
            int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
            int redeemedPoints = order.getRewardPointsRedeemed() != null ? order.getRewardPointsRedeemed() : 0;
            int earnedPoints   = order.getRewardPointsEarned()   != null ? order.getRewardPointsEarned()   : 0;

            if (redeemedPoints > 0) {
                customer.setLoyaltyPoints(currentPoints + redeemedPoints);
                currentPoints = customer.getLoyaltyPoints();

                loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                        .customer(customer)
                        .order(order)
                        .transactionType(LoyaltyTransactionType.REFUND)
                        .points(redeemedPoints)
                        .description("Refunded redeemed points for cancelled order " + order.getOrderNumber())
                        .build());
            }

            if (earnedPoints > 0) {
                int pointsToReverse = Math.min(currentPoints, earnedPoints);
                customer.setLoyaltyPoints(Math.max(0, currentPoints - pointsToReverse));

                loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                        .customer(customer)
                        .order(order)
                        .transactionType(LoyaltyTransactionType.REFUND)
                        .points(-pointsToReverse)
                        .description("Reversed earned points for cancelled order " + order.getOrderNumber())
                        .build());
            }
        }

        // Restock used coupon and remove usage record
        if (order.getAppliedCouponCode() != null && !order.getAppliedCouponCode().isBlank()) {
            couponUsageRepository.findByOrder(order).ifPresent(couponUsageRepository::delete);

            couponRepository.findByCode(order.getAppliedCouponCode()).ifPresent(coupon -> {
                int usedCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
                coupon.setUsedCount(Math.max(0, usedCount - 1));
                couponRepository.save(coupon);
            });
        }

        // Deduct from customer total spent
        if (order.getFinalAmount() != null && customer.getTotalSpent() != null) {
            customer.setTotalSpent(
                    customer.getTotalSpent().subtract(order.getFinalAmount()).max(BigDecimal.ZERO));
        }

        customerRepository.save(customer);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setStatusUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    // ── COLLECT CASH PAYMENT ─────────────────────────────────────────────
    @Override
    @Transactional
    public void collectPayment(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);
    }

    // ── SERVE WHOLE ORDER (Online Pickup) ────────────────────────────────
    @Override
    @Transactional
    public void serveOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Order is not ready yet");
        }

        // Mark all items as SERVED so line chefs see them in their Done tab
        order.getItems().forEach(item -> {
            item.setStatus(OrderItemStatus.SERVED);
            orderItemRepository.save(item);
        });

        order.updateStatus(OrderStatus.SERVED);
        orderRepository.save(order);
    }

    // ── SERVE ONE ITEM (QR dine-in) ──────────────────────────────────────
    @Override
    @Transactional
    public void serveOrderItem(Long itemId, String userEmail) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        item.setStatus(OrderItemStatus.SERVED);
        orderItemRepository.save(item);

        // If all items are now SERVED, mark the whole order as SERVED
        Order order = item.getOrder();
        boolean allServed = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.SERVED);

        if (allServed) {
            order.updateStatus(OrderStatus.SERVED);
            orderRepository.save(order);
        }
    }
}
