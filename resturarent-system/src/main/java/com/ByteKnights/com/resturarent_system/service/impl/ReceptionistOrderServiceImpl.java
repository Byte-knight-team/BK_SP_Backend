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
        private final WebSocketNotificationService webSocketNotificationService;

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
                LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

                List<Order> orders = orderRepository
                                .findByBranchIdAndStatusAndOrderTypeInAndCreatedAtBetween(
                                                branchId,
                                                orderStatus,
                                                List.of(OrderType.QR, OrderType.ONLINE_PICKUP),
                                                startOfDay,
                                                endOfDay);

                List<ReceptionistOrderSummaryDTO> result = new ArrayList<>();
                for (Order order : orders) {
                        String customerName = (order.getContactName() != null && !order.getContactName().isBlank())
                                        ? order.getContactName()
                                        : (order.getCustomer().getUser().getFullName() != null
                                                        ? order.getCustomer().getUser().getFullName()
                                                        : "Guest");

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
                                        customerName,
                                        customerPhone,
                                        tableNumber,
                                        totalItems,
                                        finalAmount));
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
                                                                : item.getUnitPrice().doubleValue()
                                                                                * item.getQuantity(),
                                                item.getStatus().name()))
                                .toList();

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
                                order.getCancelReason());
        }

        // ── STOCK CHECK (read-only) ──────────────────────────────────────────
        @Override
        public StockCheckResultDTO checkStock(Long orderId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                List<StockCheckResultDTO.StockItemResult> results = new ArrayList<>();
                boolean allSufficient = true;

                for (OrderItem orderItem : order.getItems()) {
                        if (orderItem.getMenuItem() == null)
                                continue;

                        List<MenuItemIngredient> ingredients = menuItemIngredientRepository
                                        .findByMenuItemId(orderItem.getMenuItem().getId());

                        if (ingredients.isEmpty())
                                continue;

                        List<String> shortages = new ArrayList<>();
                        for (MenuItemIngredient ing : ingredients) {
                                InventoryItem stock = ing.getInventoryItem();
                                BigDecimal needed = ing.getQuantityRequired()
                                                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));
                                BigDecimal available = stock.getQuantity() != null ? stock.getQuantity()
                                                : BigDecimal.ZERO;

                                if (available.compareTo(needed) < 0) {
                                        shortages.add(String.format("%s: need %.3f %s, have %.3f %s",
                                                        stock.getName(), needed, stock.getUnit(), available,
                                                        stock.getUnit()));
                                }
                        }

                        boolean sufficient = shortages.isEmpty();
                        if (!sufficient)
                                allSufficient = false;

                        results.add(new StockCheckResultDTO.StockItemResult(
                                        orderItem.getItemName(),
                                        sufficient,
                                        sufficient ? null : String.join(", ", shortages)));
                }

                return new StockCheckResultDTO(allSufficient, results);
        }

        // ── SEND TO KITCHEN: PLACED → PENDING ───────────────────────────────
        @Override
        @Transactional
        public void sendToKitchen(Long orderId, String userEmail) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                if (order.getStatus() != OrderStatus.PLACED) {
                        throw new RuntimeException("Order is not in PLACED status");
                }

                order.updateStatus(OrderStatus.PENDING);
                orderRepository.save(order);
                webSocketNotificationService.broadcastOrderStatusUpdate(order.getId(), order.getStatus().name());
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
                webSocketNotificationService.broadcastOrderStatusUpdate(order.getId(), order.getStatus().name());
        }

        // ── CANCEL ───────────────────────────────────────────────────────────
        @Override
        @Transactional
        public void cancelOrder(Long orderId, String reason, String userEmail) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelReason(reason);
                order.setStatusUpdatedAt(java.time.LocalDateTime.now());
                orderRepository.save(order);
                webSocketNotificationService.broadcastOrderStatusUpdate(order.getId(), order.getStatus().name());
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

                order.updateStatus(OrderStatus.SERVED);
                orderRepository.save(order);
                webSocketNotificationService.broadcastOrderStatusUpdate(order.getId(), order.getStatus().name());
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
                        webSocketNotificationService.broadcastOrderStatusUpdate(order.getId(),
                                        order.getStatus().name());
                }
        }
}
