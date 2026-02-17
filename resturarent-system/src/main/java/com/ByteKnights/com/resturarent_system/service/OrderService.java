package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.*;
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderItem;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

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
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> OrderItemDTO.builder()
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
    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public com.ByteKnights.com.resturarent_system.dto.OrderDTO cancelOrder(Long orderId, String reason) {
        Optional<Order> orderOptional = orderRepository.findById(orderId);
        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelReason(reason);
            // Optionally, we could also cancel all items if we were tracking item status
            // order.getItems().forEach(item -> item.setStatus("cancelled"));
            Order savedOrder = orderRepository.save(order);
            return mapToDTO(savedOrder);
        } else {
            throw new RuntimeException("Order not found with id: " + orderId);
        }
    }

    public java.util.List<com.ByteKnights.com.resturarent_system.dto.OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public Optional<com.ByteKnights.com.resturarent_system.dto.OrderDTO> getOrderById(Long id) {
        return orderRepository.findById(id).map(this::mapToDTO);
    }

    private com.ByteKnights.com.resturarent_system.dto.OrderDTO mapToDTO(Order order) {
        return com.ByteKnights.com.resturarent_system.dto.OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .orderType(order.getOrderType())
                .totalAmount(order.getTotalAmount())
                .cancelReason(order.getCancelReason())
                .createdAt(order.getCreatedAt())
                .branchName(order.getBranch() != null ? order.getBranch().getName() : null)
                .customerName(order.getCustomer() != null && order.getCustomer().getUser() != null
                        ? order.getCustomer().getUser().getUsername()
                        : "Guest")
                .tableNumber(order.getTable() != null ? order.getTable().getTableNumber().toString() : null)
                .items(order.getItems().stream()
                        .map(item -> com.ByteKnights.com.resturarent_system.dto.OrderDTO.OrderItemDTO.builder()
                                .id(item.getId())
                                .menuItemName(item.getMenuItem() != null ? item.getMenuItem().getName() : "Unknown")
                                .categoryName(item.getMenuItem() != null && item.getMenuItem().getCategory() != null
                                        ? item.getMenuItem().getCategory()
                                        : "General")
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }
}
