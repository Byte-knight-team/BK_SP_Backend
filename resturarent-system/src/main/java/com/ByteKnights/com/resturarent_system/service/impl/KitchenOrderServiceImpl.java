package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderCardDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderItemDetailsDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KitchenOrderServiceImpl implements KitchenOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final AuditLogService auditLogService;

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
            orderCardDetailsDTOS.add(new OrderCardDetailsDTO(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus().name(),
                    order.getStatusUpdatedAt() != null ? order.getStatusUpdatedAt().format(formatter) : null,
                    order.getItems().size()
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
                order.getOrderNumber(),
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

        Map<String, Object> oldValues = buildKitchenOrderItemAuditSnapshot(item);

        Staff previousChef = item.getAssignedLineChef();

        item.setAssignedLineChef(chef);
        OrderItem savedItem = orderItemRepository.save(item);

        // Notify the old chef that this item was reassigned
        if (previousChef != null && !previousChef.getId().equals(chef.getId())) {
            webSocketNotificationService.broadcastLineChefItemRemoved(
                    previousChef.getUser().getId(),
                    item.getItemName(),
                    item.getOrder().getOrderNumber(),
                    chef.getUser().getFullName()
            );
        }

        // Notify the new chef that an item was assigned to them
        webSocketNotificationService.broadcastLineChefItemAssigned(
                chef.getUser().getId(),
                item.getOrder().getOrderNumber(),
                item.getItemName()
        );

        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.CHEF_ASSIGNED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ORDER_ITEM,
                savedItem.getId(),
                getBranchIdFromOrderItem(savedItem),
                "Chef assigned to meal successfully",
                oldValues,
                buildKitchenOrderItemAuditSnapshot(savedItem));
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

        Map<String, Object> oldValues = buildKitchenOrderAuditSnapshot(order);

        order.setStatus(OrderStatus.ON_HOLD);
        order.setHoldReason(holdReason);
        order.setStatusUpdatedAt(LocalDateTime.now());

        for (OrderItem item : order.getItems()) {
            item.setStatus(OrderItemStatus.ON_HOLD);
        }

        Order savedOrder = orderRepository.save(order);

        // Notify customer (order tracking)
        webSocketNotificationService.broadcastOrderStatusUpdate(order.getId(), order.getStatus().name());
        // Notify receptionist (cross-role tab switch to Hold)
        webSocketNotificationService.broadcastOrderStatusChanged(branchId, orderId, "ON_HOLD");

        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.ORDER_ON_HOLD,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.ORDER,
                savedOrder.getId(),
                branchId,
                "Order placed on hold by kitchen staff",
                oldValues,
                buildKitchenOrderAuditSnapshot(savedOrder));
    }

    private Map<String, Object> buildKitchenOrderItemAuditSnapshot(OrderItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (item == null) {
            return snapshot;
        }

        Order order = item.getOrder();
        Staff assignedChef = item.getAssignedLineChef();

        snapshot.put("orderItemId", item.getId());
        snapshot.put("orderId", order != null ? order.getId() : null);
        snapshot.put("orderNumber", order != null ? order.getOrderNumber() : null);
        snapshot.put("branchId",
                order != null && order.getBranch() != null ? order.getBranch().getId() : null);

        snapshot.put("itemName", item.getItemName());
        snapshot.put("quantity", item.getQuantity());
        snapshot.put("itemStatus", item.getStatus() != null ? item.getStatus().name() : null);

        snapshot.put("assignedChefStaffId", assignedChef != null ? assignedChef.getId() : null);
        snapshot.put("assignedChefUserId",
                assignedChef != null && assignedChef.getUser() != null
                        ? assignedChef.getUser().getId()
                        : null);
        snapshot.put("assignedChefName",
                assignedChef != null && assignedChef.getUser() != null
                        ? assignedChef.getUser().getFullName()
                        : null);

        snapshot.put("cookingStartedAt", item.getCookingStartedAt());
        snapshot.put("cookingCompletedAt", item.getCookingCompletedAt());

        return snapshot;
    }

    private Map<String, Object> buildKitchenOrderAuditSnapshot(Order order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (order == null) {
            return snapshot;
        }

        snapshot.put("orderId", order.getId());
        snapshot.put("orderNumber", order.getOrderNumber());
        snapshot.put("branchId", order.getBranch() != null ? order.getBranch().getId() : null);
        snapshot.put("orderStatus", order.getStatus() != null ? order.getStatus().name() : null);
        snapshot.put("holdReason", order.getHoldReason());
        snapshot.put("kitchenNotes", order.getKitchenNotes());
        snapshot.put("createdAt", order.getCreatedAt());
        snapshot.put("statusUpdatedAt", order.getStatusUpdatedAt());

        List<Map<String, Object>> itemSnapshots = new ArrayList<>();

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                itemSnapshots.add(buildKitchenOrderItemAuditSnapshot(item));
            }
        }

        snapshot.put("items", itemSnapshots);

        return snapshot;
    }

    private Long getBranchIdFromOrderItem(OrderItem item) {
        if (item == null || item.getOrder() == null || item.getOrder().getBranch() == null) {
            return null;
        }

        return item.getOrder().getBranch().getId();
    }
}
