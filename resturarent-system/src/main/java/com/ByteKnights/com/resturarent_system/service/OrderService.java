package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.*;
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderItem;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.OrderType;
import com.ByteKnights.com.resturarent_system.entity.PaymentStatus;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final BranchRepository branchRepository;

    // ── Writes ─────────────────────────────────────────────

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        Order order = new Order();
        
        // Generate a simple unique order number. E.g., ORD-<UUID-prefix>
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerName(request.getCustomerName());
        
        // If it's a delivery order, map address somewhere (often tableNumber is reused or a new field is needed, keeping it simple)
        if ("delivery".equalsIgnoreCase(request.getOrderType())) {
            order.setOrderType(OrderType.ONLINE);
        } else {
            order.setOrderType(OrderType.QR);
        }

        order.setStatus(OrderStatus.PLACED);

        if ("pay-now".equalsIgnoreCase(request.getPaymentMethod())) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(PaymentStatus.PENDING);
        }

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId()).orElse(null);
        }
        order.setBranch(branch);

        // Process items
        List<OrderItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
                MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                        .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemReq.getMenuItemId()));
                
                OrderItem item = OrderItem.builder()
                        .itemName(menuItem.getName())
                        .menuItem(menuItem)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(menuItem.getPrice())
                        .subtotal(menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())))
                        .build();
                item.setOrder(order);
                items.add(item);
            }
        }
        order.setItems(items);
        order.recalculateTotal();

        Order saved = orderRepository.save(order);
        return toDTO(saved);
    }

    // ── Reads ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return toDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getRecentOrders() {
        return orderRepository.findTop5ByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(String status) {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        return orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderStatsDTO getOrderStats() {
        return OrderStatsDTO.builder()
                .openCount(orderRepository.countByStatus(OrderStatus.OPEN))
                .paidCount(orderRepository.countByStatus(OrderStatus.PAID))
                .closedCount(orderRepository.countByStatus(OrderStatus.CLOSED))
                .cancelledCount(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .build();
    }

    // ── Writes ─────────────────────────────────────────────

    @Transactional
    public OrderDTO updateOrder(Long id, UpdateOrderRequest request) {
        Order order = orderRepository.findOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        // Only OPEN orders can be edited
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new IllegalStateException(
                    "Cannot edit order in " + order.getStatus() + " status. Only OPEN orders can be modified.");
        }

        if (request.getTableNumber() != null) {
            order.setTableNumber(request.getTableNumber());
        }

        if (request.getItems() != null) {
            order.getItems().clear();

            List<OrderItem> newItems = new ArrayList<>();
            for (UpdateOrderRequest.UpdateOrderItemRequest itemReq : request.getItems()) {
                OrderItem item = OrderItem.builder()
                        .itemName(itemReq.getItemName())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(itemReq.getUnitPrice())
                        .kitchenNotes(itemReq.getKitchenNotes())
                        .order(order)
                        .build();
                newItems.add(item);
            }
            order.getItems().addAll(newItems);
            order.recalculateTotal();
        }

        Order saved = orderRepository.save(order);
        return toDTO(saved);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long id, String newStatus) {
        Order order = orderRepository.findOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid status: " + newStatus);
        }

        // Validate allowed transitions
        OrderStatus current = order.getStatus();
        boolean allowed = switch (current) {
            case OPEN -> targetStatus == OrderStatus.PAID || targetStatus == OrderStatus.CANCELLED;
            case PAID -> targetStatus == OrderStatus.CLOSED;
            default -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Cannot transition from " + current + " to " + targetStatus);
        }

        order.setStatus(targetStatus);
        Order saved = orderRepository.save(order);
        return toDTO(saved);
    }

    @Transactional
    public OrderDTO cancelOrder(Long id, String reason) {
        Order order = orderRepository.findOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        // Prevent cancelling already closed or cancelled orders
        if (order.getStatus() == OrderStatus.CLOSED) {
            throw new IllegalStateException("Cannot cancel a closed order.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setTotal(BigDecimal.ZERO);
        order.setCancellationReason(reason);
        Order saved = orderRepository.save(order);
        return toDTO(saved);
    }

    // ── Mapper ─────────────────────────────────────────────

    private OrderDTO toDTO(Order order) {
        List<OrderDTO.OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> OrderDTO.OrderItemDTO.builder()
                        .id(item.getId())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .kitchenNotes(item.getKitchenNotes())
                        .build())
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .tableNumber(order.getTableNumber())
                .guestCount(order.getGuestCount())
                .status(order.getStatus().name())
                .total(order.getTotal())
                .cancellationReason(order.getCancellationReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemDTOs)
                .build();
    }
}
