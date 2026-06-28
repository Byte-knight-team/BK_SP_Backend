package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderCardDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderItemDetailsDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.KitchenOrderService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KitchenOrderServiceImpl implements KitchenOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ChefAttendanceRepository chefAttendanceRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Override
    public List<OrderCardDetailsDTO> getOrdersByStatus(OrderStatus status, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();
        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIN);

        Sort sort = (status == OrderStatus.COMPLETED)
                ? Sort.by(Sort.Direction.DESC, "statusUpdatedAt")
                : Sort.by(Sort.Direction.ASC, "statusUpdatedAt");

        List<Order> orders = orderRepository.findByBranchIdAndStatusAndStatusUpdatedAtAfter(
                branchId, status, startOfToday, sort);

        List<OrderCardDetailsDTO> orderCardDetailsDTOS = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (Order order : orders) {
            int totalQty = 0;
            for (OrderItem item : order.getItems()) {
                totalQty += item.getQuantity();
            }
            orderCardDetailsDTOS.add(new OrderCardDetailsDTO(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus().name(),
                    order.getStatusUpdatedAt() != null ? order.getStatusUpdatedAt().format(formatter) : null,
                    totalQty
            ));
        }
        return orderCardDetailsDTOS;
    }

    @Override
    public OrderDetailsDTO getOrderDetails(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        Order order = orderRepository.findByIdAndBranchId(orderId, branchId)
                .orElseThrow(() -> new RuntimeException("Order not found in your branch with ID: " + orderId));

        List<OrderItemDetailsDTO> itemDTOs = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemDTOs.add(new OrderItemDetailsDTO(
                    item.getId(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getStatus().toString(),
                    item.getAssignedLineChef() != null ? item.getAssignedLineChef().getUser().getFullName() : "Not Assigned"
            ));
        }

        return new OrderDetailsDTO(
                order.getId(),
                order.getCreatedAt(),
                order.getStatusUpdatedAt(),
                order.getStatus().toString(),
                order.getHoldReason(),
                order.getKitchenNotes(),
                itemDTOs
        );
    }

    @Override
    public void assignChefToMeal(Long orderItemId, Long chefStaffId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Meal not found"));

        Staff chef = staffRepository.findById(chefStaffId)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        if (!item.getOrder().getBranch().getId().equals(chef.getBranch().getId())) {
            throw new RuntimeException("Security Alert: Cannot assign a chef from a different branch!");
        }

        // Capture old chef before overwriting so we can notify them to refresh
        Staff previousChef = item.getAssignedLineChef();

        item.setAssignedLineChef(chef);
        orderItemRepository.save(item);

        // Notify the old chef to silently remove this item from their dashboard
        if (previousChef != null && !previousChef.getId().equals(chef.getId())) {
            webSocketNotificationService.broadcastLineChefItemRemoved(
                    previousChef.getUser().getId()
            );
        }

        Long lineChefUserId = chef.getUser().getId();
        webSocketNotificationService.broadcastLineChefItemAssigned(
                lineChefUserId,
                item.getOrder().getOrderNumber(),
                item.getItemName()
        );
    }

    @Override
    @Transactional
    public void holdOrder(Long orderId, String holdReason, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        Order order = orderRepository.findByIdAndBranchId(orderId, branchId)
                .orElseThrow(() -> new RuntimeException("Order not found in your branch with ID: " + orderId));

        order.setStatus(OrderStatus.ON_HOLD);
        order.setHoldReason(holdReason);
        order.setStatusUpdatedAt(LocalDateTime.now());

        for (OrderItem item : order.getItems()) {
            item.setStatus(OrderItemStatus.ON_HOLD);
        }

        orderRepository.save(order);
    }
}
