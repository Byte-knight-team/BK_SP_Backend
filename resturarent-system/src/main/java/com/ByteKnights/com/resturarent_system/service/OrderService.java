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
public class OrderService {

    private final OrderRepository orderRepository;

    // ── Reads ──────────────────────────────────────────────

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return toDTO(order);
    }

    public List<OrderDTO> getRecentOrders() {
        return orderRepository.findTop5ByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<OrderDTO> getOrdersByStatus(String status) {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        return orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

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
        Order order = orderRepository.findById(id)
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
    public OrderDTO cancelOrder(Long id, String reason) {
        Order order = orderRepository.findById(id)
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
                .build();
    }
}
