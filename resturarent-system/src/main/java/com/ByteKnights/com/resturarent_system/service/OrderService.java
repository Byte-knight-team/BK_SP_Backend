package com.ByteKnights.com.resturarent_system.service;

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
