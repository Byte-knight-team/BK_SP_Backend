package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.MealCompletionResponseDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderCardDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderItemDetailsDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.KitchenOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final ChefAttendanceRepository chefAttendanceRepository;
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
                branchId,
                status,
                startOfToday,
                sort
        );

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
                    item.getAssignedChef() != null
                            ? item.getAssignedChef().getUser().getFullName()
                            : "Not Assigned"
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
    @Transactional
    public void assignChefToMeal(Long orderItemId, Long chefStaffId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Meal not found"));

        Staff chef = staffRepository.findById(chefStaffId)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        if (!item.getOrder().getBranch().getId().equals(chef.getBranch().getId())) {
            throw new RuntimeException("Security Alert: Cannot assign a chef from a different branch!");
        }

        /*
         * Manual audit is used here because we need old and new assigned chef details.
         * @Auditable is not used because this method returns void and the AOP targetId would be null.
         */
        Map<String, Object> oldValues = buildKitchenOrderItemAuditSnapshot(item);

        item.setAssignedChef(chef);
        OrderItem savedItem = orderItemRepository.save(item);

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
                buildKitchenOrderItemAuditSnapshot(savedItem)
        );
    }

    @Override
    @Transactional
    public void startMeal(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Meal item not found"));

        if (item.getAssignedChef() == null) {
            throw new RuntimeException("Cannot start meal: No chef assigned yet!");
        }

        Order order = item.getOrder();

        ChefAttendance attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(
                        item.getAssignedChef().getId(),
                        LocalDate.now()
                )
                .orElseThrow(() -> new RuntimeException("Chef attendance record not found for today"));

        if (attendance.getAttendanceStatus() == ChefAttendanceStatus.OFF_DUTY) {
            throw new RuntimeException("Cannot start: Chef " +
                    item.getAssignedChef().getUser().getFullName() + " has already checked out!");
        }

        if (attendance.getWorkStatus() == ChefWorkStatus.ON_BREAK) {
            throw new RuntimeException("Cannot start: Chef is currently on a break.");
        }

        /*
         * Capture old values before changing meal item, order, and chef work status.
         */
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("orderItem", buildKitchenOrderItemAuditSnapshot(item));
        oldValues.put("order", buildKitchenOrderAuditSnapshot(order));
        oldValues.put("chefAttendance", buildChefAttendanceAuditSnapshot(attendance));

        item.setStatus(OrderItemStatus.PREPARING);
        item.setCookingStartedAt(LocalDateTime.now());
        orderItemRepository.save(item);

        if (order.getStatus() == OrderStatus.PENDING) {
            order.updateStatus(OrderStatus.PREPARING);
            orderRepository.save(order);
        }

        attendance.setWorkStatus(ChefWorkStatus.COOKING);
        chefAttendanceRepository.save(attendance);

        /*
         * Capture new values after the update.
         */
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("orderItem", buildKitchenOrderItemAuditSnapshot(item));
        newValues.put("order", buildKitchenOrderAuditSnapshot(order));
        newValues.put("chefAttendance", buildChefAttendanceAuditSnapshot(attendance));

        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.MEAL_STARTED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ORDER_ITEM,
                item.getId(),
                getBranchIdFromOrderItem(item),
                "Meal preparation started successfully",
                oldValues,
                newValues
        );
    }

    @Override
    @Transactional
    public MealCompletionResponseDTO completeMeal(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Meal item not found"));

        if (item.getAssignedChef() == null) {
            throw new RuntimeException("Cannot complete meal: No chef assigned");
        }

        Order order = item.getOrder();

        ChefAttendance attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(
                        item.getAssignedChef().getId(),
                        LocalDate.now()
                )
                .orElseThrow(() -> new RuntimeException("Chef attendance not found"));

        /*
         * Capture old values before marking the meal as ready.
         */
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("orderItem", buildKitchenOrderItemAuditSnapshot(item));
        oldValues.put("order", buildKitchenOrderAuditSnapshot(order));
        oldValues.put("chefAttendance", buildChefAttendanceAuditSnapshot(attendance));

        item.setStatus(OrderItemStatus.READY);
        item.setCookingCompletedAt(LocalDateTime.now());
        orderItemRepository.save(item);

        attendance.setWorkStatus(ChefWorkStatus.AVAILABLE);
        chefAttendanceRepository.save(attendance);

        boolean allFinished = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.READY);

        if (allFinished) {
            order.updateStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
        }

        /*
         * Capture new values after meal completion.
         */
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("orderItem", buildKitchenOrderItemAuditSnapshot(item));
        newValues.put("order", buildKitchenOrderAuditSnapshot(order));
        newValues.put("chefAttendance", buildChefAttendanceAuditSnapshot(attendance));

        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.MEAL_COMPLETED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ORDER_ITEM,
                item.getId(),
                getBranchIdFromOrderItem(item),
                "Meal completed successfully",
                oldValues,
                newValues
        );

        return new MealCompletionResponseDTO(order.getStatus().toString());
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

        /*
         * Manual audit is used because order hold changes both order status
         * and all related order item statuses.
         */
        Map<String, Object> oldValues = buildKitchenOrderAuditSnapshot(order);

        order.setStatus(OrderStatus.ON_HOLD);
        order.setHoldReason(holdReason);
        order.setStatusUpdatedAt(LocalDateTime.now());

        for (OrderItem item : order.getItems()) {
            item.setStatus(OrderItemStatus.ON_HOLD);
        }

        Order savedOrder = orderRepository.save(order);

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
                buildKitchenOrderAuditSnapshot(savedOrder)
        );
    }

    /*
     * Builds a safe audit snapshot for one order item.
     *
     * Important:
     * Do not send the full entity directly to audit JSON because relationships like
     * Order -> Items -> Order can cause recursion or very large JSON.
     */
    private Map<String, Object> buildKitchenOrderItemAuditSnapshot(OrderItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (item == null) {
            return snapshot;
        }

        Order order = item.getOrder();
        Staff assignedChef = item.getAssignedChef();

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

    /*
     * Builds a safe audit snapshot for an order.
     *
     * This includes order status and item statuses so SUPER_ADMIN can clearly see
     * what changed from the audit details modal.
     */
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

    /*
     * Builds a safe audit snapshot for chef attendance and work status.
     */
    private Map<String, Object> buildChefAttendanceAuditSnapshot(ChefAttendance attendance) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (attendance == null) {
            return snapshot;
        }

        Staff staff = attendance.getStaff();

        snapshot.put("attendanceId", attendance.getId());
        snapshot.put("chefStaffId", staff != null ? staff.getId() : null);
        snapshot.put("chefUserId",
                staff != null && staff.getUser() != null ? staff.getUser().getId() : null);
        snapshot.put("chefName",
                staff != null && staff.getUser() != null ? staff.getUser().getFullName() : null);
        snapshot.put("attendanceDate", attendance.getAttendanceDate());
        snapshot.put("attendanceStatus",
                attendance.getAttendanceStatus() != null
                        ? attendance.getAttendanceStatus().name()
                        : null);
        snapshot.put("workStatus",
                attendance.getWorkStatus() != null
                        ? attendance.getWorkStatus().name()
                        : null);

        return snapshot;
    }

    /*
     * Gets branchId from the order item.
     *
     * Passing branchId manually keeps audit logs accurate for branch filtering.
     */
    private Long getBranchIdFromOrderItem(OrderItem item) {
        if (item == null || item.getOrder() == null || item.getOrder().getBranch() == null) {
            return null;
        }

        return item.getOrder().getBranch().getId();
    }
}