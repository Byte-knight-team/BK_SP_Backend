package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.*;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.ReceptionistOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReceptionistOrderServiceImpl implements ReceptionistOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final MenuItemIngredientRepository menuItemIngredientRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final AuditLogService auditLogService;

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
                        endOfDay
                );

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
                        item.getSubtotal() != null
                                ? item.getSubtotal().doubleValue()
                                : item.getUnitPrice().doubleValue() * item.getQuantity(),
                        item.getStatus().name()
                ))
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
                customerName,
                customerPhone,
                customerEmail,
                order.getContactName(),
                order.getContactPhone(),
                order.getContactEmail(),
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

    // ── STOCK CHECK (read-only) ──────────────────────────────────────────
    @Override
    public StockCheckResultDTO checkStock(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<StockCheckResultDTO.StockItemResult> results = new ArrayList<>();
        boolean allSufficient = true;

        for (OrderItem orderItem : order.getItems()) {
            if (orderItem.getMenuItem() == null) {
                continue;
            }

            List<MenuItemIngredient> ingredients =
                    menuItemIngredientRepository.findByMenuItemId(orderItem.getMenuItem().getId());

            if (ingredients.isEmpty()) {
                continue;
            }

            List<String> shortages = new ArrayList<>();

            for (MenuItemIngredient ing : ingredients) {
                InventoryItem stock = ing.getInventoryItem();

                BigDecimal needed = ing.getQuantityRequired()
                        .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

                BigDecimal available = stock.getQuantity() != null
                        ? stock.getQuantity()
                        : BigDecimal.ZERO;

                if (available.compareTo(needed) < 0) {
                    shortages.add(String.format(
                            "%s: need %.3f %s, have %.3f %s",
                            stock.getName(),
                            needed,
                            stock.getUnit(),
                            available,
                            stock.getUnit()
                    ));
                }
            }

            boolean sufficient = shortages.isEmpty();

            if (!sufficient) {
                allSufficient = false;
            }

            results.add(new StockCheckResultDTO.StockItemResult(
                    orderItem.getItemName(),
                    sufficient,
                    sufficient ? null : String.join(", ", shortages)
            ));
        }

        return new StockCheckResultDTO(allSufficient, results);
    }

    // ── SEND TO KITCHEN: PLACED → PENDING ───────────────────────────────
    @Override
    @Transactional
    public void sendToKitchen(Long orderId, String userEmail) {
        Long actorBranchId = getBranchId(userEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        ensureOrderBelongsToBranch(order, actorBranchId);

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new RuntimeException("Order is not in PLACED status");
        }

        /*
         * Manual audit is used because this changes order status.
         */
        Map<String, Object> oldValues = buildReceptionistOrderAuditSnapshot(order);

        order.updateStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        auditLogService.logCurrentUserAction(
                AuditModule.ORDER,
                AuditEventType.ORDER_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ORDER,
                savedOrder.getId(),
                getOrderBranchId(savedOrder),
                "Order sent to kitchen successfully",
                oldValues,
                buildReceptionistOrderAuditSnapshot(savedOrder)
        );
    }

    // ── HOLD ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void holdOrder(Long orderId, String reason, String userEmail) {
        Long actorBranchId = getBranchId(userEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        ensureOrderBelongsToBranch(order, actorBranchId);

        /*
         * Manual audit is used because this changes order status and hold reason.
         */
        Map<String, Object> oldValues = buildReceptionistOrderAuditSnapshot(order);

        order.setStatus(OrderStatus.ON_HOLD);
        order.setHoldReason(reason);
        order.setStatusUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        auditLogService.logCurrentUserAction(
                AuditModule.ORDER,
                AuditEventType.ORDER_ON_HOLD,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.ORDER,
                savedOrder.getId(),
                getOrderBranchId(savedOrder),
                "Order placed on hold by receptionist",
                oldValues,
                buildReceptionistOrderAuditSnapshot(savedOrder)
        );
    }

    // ── CANCEL ───────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void cancelOrder(Long orderId, String reason, String userEmail) {
        Long actorBranchId = getBranchId(userEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        ensureOrderBelongsToBranch(order, actorBranchId);

        /*
         * Manual audit is used because this changes order status and cancel reason.
         */
        Map<String, Object> oldValues = buildReceptionistOrderAuditSnapshot(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setStatusUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        auditLogService.logCurrentUserAction(
                AuditModule.ORDER,
                AuditEventType.ORDER_CANCELLED,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.ORDER,
                savedOrder.getId(),
                getOrderBranchId(savedOrder),
                "Order cancelled by receptionist",
                oldValues,
                buildReceptionistOrderAuditSnapshot(savedOrder)
        );
    }

    // ── COLLECT CASH PAYMENT ─────────────────────────────────────────────
    @Override
    @Transactional
    public void collectPayment(Long orderId, String userEmail) {
        Long actorBranchId = getBranchId(userEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        ensureOrderBelongsToBranch(order, actorBranchId);

        /*
         * Manual audit is used because payment status updates need old/new values.
         */
        Map<String, Object> oldValues = buildReceptionistOrderAuditSnapshot(order);

        order.setPaymentStatus(PaymentStatus.PAID);

        Order savedOrder = orderRepository.save(order);

        auditLogService.logCurrentUserAction(
                AuditModule.PAYMENT,
                AuditEventType.PAYMENT_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.PAYMENT,
                savedOrder.getId(),
                getOrderBranchId(savedOrder),
                "Cash payment collected successfully",
                oldValues,
                buildReceptionistOrderAuditSnapshot(savedOrder)
        );
    }

    // ── SERVE WHOLE ORDER (Online Pickup) ────────────────────────────────
    @Override
    @Transactional
    public void serveOrder(Long orderId, String userEmail) {
        Long actorBranchId = getBranchId(userEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        ensureOrderBelongsToBranch(order, actorBranchId);

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Order is not ready yet");
        }

        /*
         * Manual audit is used because this changes the whole order status.
         */
        Map<String, Object> oldValues = buildReceptionistOrderAuditSnapshot(order);

        order.updateStatus(OrderStatus.SERVED);

        Order savedOrder = orderRepository.save(order);

        auditLogService.logCurrentUserAction(
                AuditModule.ORDER,
                AuditEventType.ORDER_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ORDER,
                savedOrder.getId(),
                getOrderBranchId(savedOrder),
                "Order served successfully",
                oldValues,
                buildReceptionistOrderAuditSnapshot(savedOrder)
        );
    }

    // ── SERVE ONE ITEM (QR dine-in) ──────────────────────────────────────
    @Override
    @Transactional
    public void serveOrderItem(Long itemId, String userEmail) {
        Long actorBranchId = getBranchId(userEmail);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        Order order = item.getOrder();

        ensureOrderBelongsToBranch(order, actorBranchId);

        /*
         * Manual audit is used because this changes one order item status.
         * We also include the parent order state because the whole order may become SERVED.
         */
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("orderItem", buildReceptionistOrderItemAuditSnapshot(item));
        oldValues.put("order", buildReceptionistOrderAuditSnapshot(order));

        item.setStatus(OrderItemStatus.SERVED);

        OrderItem savedItem = orderItemRepository.save(item);

        // If all items are now SERVED, mark the whole order as SERVED
        boolean allServed = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.SERVED);

        if (allServed) {
            order.updateStatus(OrderStatus.SERVED);
            orderRepository.save(order);
        }

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("orderItem", buildReceptionistOrderItemAuditSnapshot(savedItem));
        newValues.put("order", buildReceptionistOrderAuditSnapshot(order));

        auditLogService.logCurrentUserAction(
                AuditModule.ORDER,
                AuditEventType.ORDER_ITEM_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ORDER_ITEM,
                savedItem.getId(),
                getOrderBranchId(order),
                "Order item served successfully",
                oldValues,
                newValues
        );
    }

    /*
     * Extra branch safety for receptionist actions.
     * Receptionist should only update orders from their own branch.
     */
    private void ensureOrderBelongsToBranch(Order order, Long actorBranchId) {
        Long orderBranchId = getOrderBranchId(order);

        if (orderBranchId == null || !orderBranchId.equals(actorBranchId)) {
            throw new RuntimeException("Security Alert: Access Denied! This order does not belong to your branch.");
        }
    }

    /*
     * Gets branch ID from an order safely.
     */
    private Long getOrderBranchId(Order order) {
        if (order == null || order.getBranch() == null) {
            return null;
        }

        return order.getBranch().getId();
    }

    /*
     * Builds safe audit JSON for receptionist order actions.
     * We do not store the full entity because Order has many relationships.
     */
    private Map<String, Object> buildReceptionistOrderAuditSnapshot(Order order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (order == null) {
            return snapshot;
        }

        snapshot.put("orderId", order.getId());
        snapshot.put("orderNumber", order.getOrderNumber());
        snapshot.put("branchId", getOrderBranchId(order));
        snapshot.put("branchName", order.getBranch() != null ? order.getBranch().getName() : null);
        snapshot.put("orderType", order.getOrderType() != null ? order.getOrderType().name() : null);
        snapshot.put("orderStatus", order.getStatus() != null ? order.getStatus().name() : null);
        snapshot.put("paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);

        snapshot.put("contactName", order.getContactName());
        snapshot.put("contactPhone", order.getContactPhone());
        snapshot.put("contactEmail", order.getContactEmail());

        snapshot.put("tableId", order.getTable() != null ? order.getTable().getId() : null);
        snapshot.put("tableNumber", order.getTable() != null ? order.getTable().getTableNumber() : null);

        snapshot.put("totalAmount", order.getTotalAmount());
        snapshot.put("taxAmount", order.getTaxAmount());
        snapshot.put("serviceCharge", order.getServiceCharge());
        snapshot.put("discountAmount", order.getDiscountAmount());
        snapshot.put("appliedCouponCode", order.getAppliedCouponCode());
        snapshot.put("finalAmount", order.getFinalAmount());

        snapshot.put("kitchenNotes", order.getKitchenNotes());
        snapshot.put("holdReason", order.getHoldReason());
        snapshot.put("cancelReason", order.getCancelReason());

        snapshot.put("createdAt", order.getCreatedAt());
        snapshot.put("statusUpdatedAt", order.getStatusUpdatedAt());

        List<Map<String, Object>> itemSnapshots = new ArrayList<>();

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                itemSnapshots.add(buildReceptionistOrderItemAuditSnapshot(item));
            }
        }

        snapshot.put("items", itemSnapshots);

        return snapshot;
    }

    /*
     * Builds safe audit JSON for one order item.
     */
    private Map<String, Object> buildReceptionistOrderItemAuditSnapshot(OrderItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (item == null) {
            return snapshot;
        }

        snapshot.put("orderItemId", item.getId());
        snapshot.put("orderId", item.getOrder() != null ? item.getOrder().getId() : null);
        snapshot.put("orderNumber", item.getOrder() != null ? item.getOrder().getOrderNumber() : null);
        snapshot.put("itemName", item.getItemName());
        snapshot.put("quantity", item.getQuantity());
        snapshot.put("unitPrice", item.getUnitPrice());
        snapshot.put("subtotal", item.getSubtotal());
        snapshot.put("itemStatus", item.getStatus() != null ? item.getStatus().name() : null);
        snapshot.put("menuItemId", item.getMenuItem() != null ? item.getMenuItem().getId() : null);

        return snapshot;
    }
}