package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefCookingStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefHistoryItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.PagedResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.LineChefService;
import com.ByteKnights.com.resturarent_system.service.ManagerNotificationService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LineChefServiceImpl implements LineChefService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ChefAttendanceRepository chefAttendanceRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final ManagerNotificationService managerNotificationService;

    // Audit logging
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a");

    private Staff getStaffFromEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
    }

    // =========================================================
    // GET CURRENT LINE CHEF ITEMS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<LineChefItemDTO> getMyItems(String userEmail) {

        Staff lineChef = getStaffFromEmail(userEmail);

        List<OrderItem> activeItems =
                orderItemRepository.findByAssignedLineChefIdAndStatusIn(
                        lineChef.getId(),
                        List.of(
                                OrderItemStatus.PENDING,
                                OrderItemStatus.PREPARING
                        )
                );

        /*
         * Include today's READY and SERVED items.
         * cookingCompletedAt is checked so old completed items
         * are not included in the current workload.
         */
        List<OrderItem> todayDoneItems =
                orderItemRepository.findByAssignedLineChefIdAndStatusIn(
                                lineChef.getId(),
                                List.of(
                                        OrderItemStatus.READY,
                                        OrderItemStatus.SERVED
                                )
                        )
                        .stream()
                        .filter(item ->
                                item.getCookingCompletedAt() != null
                                        && item.getCookingCompletedAt()
                                        .toLocalDate()
                                        .equals(LocalDate.now())
                        )
                        .toList();

        List<OrderItem> items = new ArrayList<>();

        items.addAll(activeItems);
        items.addAll(todayDoneItems);

        return items.stream()
                .map(item -> {

                    Order order = item.getOrder();

                    Integer tableNumber =
                            order.getTable() != null
                                    ? order.getTable().getTableNumber()
                                    : null;

                    String placedAt =
                            order.getCreatedAt() != null
                                    ? order.getCreatedAt().format(FORMATTER)
                                    : "";

                    return new LineChefItemDTO(
                            item.getId(),
                            item.getItemName(),
                            item.getQuantity(),
                            item.getStatus().name(),

                            order.getId(),
                            order.getOrderNumber(),
                            order.getOrderType().name(),

                            tableNumber,
                            placedAt,

                            item.getKitchenNotes(),
                            order.getKitchenNotes()
                    );
                })
                .toList();
    }

    // =========================================================
    // START PREPARING ITEM
    // =========================================================

    @Override
    @Transactional
    public void startItem(Long itemId, String userEmail) {

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Staff lineChef = getStaffFromEmail(userEmail);

        /*
         * Ensure the current line chef owns this item.
         */
        if (item.getAssignedLineChef() == null
                || !item.getAssignedLineChef()
                .getId()
                .equals(lineChef.getId())) {

            throw new RuntimeException("This item is not assigned to you");
        }

        /*
         * Only PENDING items can start cooking.
         */
        if (item.getStatus() != OrderItemStatus.PENDING) {
            throw new RuntimeException("Item is not in PENDING status");
        }

        /*
         * Line chef must have today's attendance record.
         */
        ChefAttendance attendance =
                chefAttendanceRepository
                        .findByStaffIdAndAttendanceDate(
                                lineChef.getId(),
                                LocalDate.now()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No attendance record for today. Please check in first."
                                )
                        );

        Order order = item.getOrder();

        /*
         * Capture old values before changing entities.
         */
        Map<String, Object> oldValues = new LinkedHashMap<>();

        oldValues.put(
                "orderItem",
                buildOrderItemAuditSnapshot(item)
        );

        oldValues.put(
                "order",
                buildOrderAuditSnapshot(order)
        );

        oldValues.put(
                "chefAttendance",
                buildChefAttendanceAuditSnapshot(attendance)
        );

        /*
         * Update order item.
         */
        item.setStatus(OrderItemStatus.PREPARING);
        item.setCookingStartedAt(LocalDateTime.now());

        OrderItem savedItem = orderItemRepository.save(item);

        /*
         * If order itself is still pending,
         * move it into PREPARING.
         */
        if (order.getStatus() == OrderStatus.PENDING) {

            order.updateStatus(OrderStatus.PREPARING);
            orderRepository.save(order);
        }

        /*
         * Update chef work state.
         */
        attendance.setWorkStatus(ChefWorkStatus.COOKING);

        ChefAttendance savedAttendance =
                chefAttendanceRepository.save(attendance);

        /*
         * Capture values after update.
         */
        Map<String, Object> newValues = new LinkedHashMap<>();

        newValues.put(
                "orderItem",
                buildOrderItemAuditSnapshot(savedItem)
        );

        newValues.put(
                "order",
                buildOrderAuditSnapshot(order)
        );

        newValues.put(
                "chefAttendance",
                buildChefAttendanceAuditSnapshot(savedAttendance)
        );

        /*
         * Audit the action.
         */
        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.MEAL_STARTED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ORDER_ITEM,

                savedItem.getId(),
                getOrderBranchId(order),

                "Line chef started preparing order item",

                oldValues,
                newValues
        );

        /*
         * Broadcast live kitchen update.
         */
        Long branchId = getOrderBranchId(order);

        if (branchId != null) {

            webSocketNotificationService.broadcastKitchenItemUpdate(
                    branchId,
                    order.getId(),
                    order.getOrderNumber(),
                    item.getItemName(),

                    "PREPARING",

                    order.getStatus().name(),
                    order.getOrderType().name()
            );
        }
    }

    // =========================================================
    // COMPLETE ITEM
    // =========================================================

    @Override
    @Transactional
    public void completeItem(Long itemId, String userEmail) {

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Staff lineChef = getStaffFromEmail(userEmail);

        /*
         * Verify ownership.
         */
        if (item.getAssignedLineChef() == null
                || !item.getAssignedLineChef()
                .getId()
                .equals(lineChef.getId())) {

            throw new RuntimeException("This item is not assigned to you");
        }

        /*
         * Only PREPARING items can be completed.
         */
        if (item.getStatus() != OrderItemStatus.PREPARING) {

            throw new RuntimeException(
                    "Item is not being prepared"
            );
        }

        Order order = item.getOrder();

        /*
         * Capture old state.
         */
        Map<String, Object> oldValues = new LinkedHashMap<>();

        oldValues.put(
                "orderItem",
                buildOrderItemAuditSnapshot(item)
        );

        oldValues.put(
                "order",
                buildOrderAuditSnapshot(order)
        );

        /*
         * Mark item READY.
         */
        item.setStatus(OrderItemStatus.READY);
        item.setCookingCompletedAt(LocalDateTime.now());

        OrderItem savedItem =
                orderItemRepository.save(item);

        /*
         * Check whether every item has finished cooking / serving.
         */
        boolean allFinished =
                order.getItems()
                        .stream()
                        .allMatch(i ->
                                i.getStatus() == OrderItemStatus.READY
                                        || i.getStatus() == OrderItemStatus.SERVED
                        );

        if (allFinished) {

            order.updateStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);

            /*
             * Completed online delivery orders must notify manager
             * so dispatch can begin.
             */
            if (order.getOrderType() == OrderType.ONLINE_DELIVERY) {

                Long branchId = getOrderBranchId(order);

                if (branchId != null) {

                    managerNotificationService.createNotification(
                            branchId,
                            ManagerNotificationType.NEW_DELIVERY,

                            "Order "
                                    + order.getOrderNumber()
                                    + " is COMPLETED and ready for dispatch!",

                            order.getId()
                    );
                }
            }
        }

        /*
         * If chef has no more PREPARING items,
         * return their work status to AVAILABLE.
         */
        ChefAttendance savedAttendance = null;

        List<OrderItem> stillPreparing =
                orderItemRepository.findByAssignedLineChefIdAndStatusIn(
                        lineChef.getId(),
                        List.of(OrderItemStatus.PREPARING)
                );

        if (stillPreparing.isEmpty()) {

            ChefAttendance attendance =
                    chefAttendanceRepository
                            .findByStaffIdAndAttendanceDate(
                                    lineChef.getId(),
                                    LocalDate.now()
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "No attendance record found"
                                    )
                            );

            /*
             * Include attendance old state in audit.
             */
            oldValues.put(
                    "chefAttendance",
                    buildChefAttendanceAuditSnapshot(attendance)
            );

            attendance.setWorkStatus(
                    ChefWorkStatus.AVAILABLE
            );

            savedAttendance =
                    chefAttendanceRepository.save(attendance);
        }

        /*
         * Capture updated state.
         */
        Map<String, Object> newValues = new LinkedHashMap<>();

        newValues.put(
                "orderItem",
                buildOrderItemAuditSnapshot(savedItem)
        );

        newValues.put(
                "order",
                buildOrderAuditSnapshot(order)
        );

        newValues.put(
                "chefAttendance",
                buildChefAttendanceAuditSnapshot(savedAttendance)
        );

        /*
         * Audit completed meal.
         */
        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.MEAL_COMPLETED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.ORDER_ITEM,

                savedItem.getId(),
                getOrderBranchId(order),

                "Line chef completed preparing order item",

                oldValues,
                newValues
        );

        /*
         * Live updates.
         */
        Long branchId = getOrderBranchId(order);

        if (branchId != null) {

            webSocketNotificationService.broadcastKitchenItemUpdate(
                    branchId,
                    order.getId(),
                    order.getOrderNumber(),
                    item.getItemName(),

                    "READY",

                    order.getStatus().name(),
                    order.getOrderType().name()
            );

            /*
             * QR order:
             * update receptionist table monitor.
             */
            if (order.getOrderType() == OrderType.QR) {

                webSocketNotificationService
                        .broadcastTableUpdate(branchId);
            }
        }
    }

    // =========================================================
    // COOKING HISTORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<LineChefHistoryItemDTO> getCookingHistory(
            String userEmail,
            int page,
            int size,
            String date,
            String status
    ) {

        Staff lineChef =
                getStaffFromEmail(userEmail);

        /*
         * Optional date filter.
         *
         * yyyy-MM-dd
         *
         * Example:
         * 2026-08-17
         */
        LocalDateTime dayStart = null;
        LocalDateTime dayEnd = null;

        if (date != null && !date.isBlank()) {

            LocalDate day =
                    LocalDate.parse(date);

            dayStart =
                    day.atStartOfDay();

            dayEnd =
                    dayStart.plusDays(1);
        }

        /*
         * Optional finished status filter.
         */
        OrderItemStatus statusFilter = null;

        if (status != null && !status.isBlank()) {

            statusFilter =
                    OrderItemStatus.valueOf(
                            status.toUpperCase()
                    );
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        Page<OrderItem> result =
                orderItemRepository.findHistoryByLineChef(
                        lineChef.getId(),
                        statusFilter,
                        dayStart,
                        dayEnd,
                        pageable
                );

        List<LineChefHistoryItemDTO> content =
                result.getContent()
                        .stream()
                        .map(item -> {

                            Order order =
                                    item.getOrder();

                            Integer tableNumber =
                                    order.getTable() != null
                                            ? order.getTable().getTableNumber()
                                            : null;

                            return new LineChefHistoryItemDTO(
                                    item.getId(),

                                    item.getItemName(),

                                    item.getQuantity(),

                                    item.getStatus().name(),

                                    order.getOrderNumber(),

                                    tableNumber,

                                    item.getCookingCompletedAt()
                                            .format(DATE_FORMATTER),

                                    item.getCookingStartedAt() != null
                                            ? item.getCookingStartedAt()
                                            .format(TIME_FORMATTER)
                                            : "-",

                                    item.getCookingCompletedAt()
                                            .format(TIME_FORMATTER)
                            );
                        })
                        .toList();

        return PagedResponse
                .<LineChefHistoryItemDTO>builder()

                .content(content)

                .page(
                        result.getNumber()
                )

                .size(
                        result.getSize()
                )

                .totalElements(
                        result.getTotalElements()
                )

                .totalPages(
                        result.getTotalPages()
                )

                .build();
    }

    // =========================================================
    // COOKING STATISTICS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public LineChefCookingStatsDTO getCookingStats(
            String userEmail
    ) {

        Staff lineChef =
                getStaffFromEmail(userEmail);

        LocalDateTime startOfToday =
                LocalDate.now()
                        .atStartOfDay();

        LocalDateTime endOfToday =
                startOfToday.plusDays(1);

        long cookedToday =
                orderItemRepository
                        .countByAssignedLineChefIdAndCookingCompletedAtBetween(
                                lineChef.getId(),
                                startOfToday,
                                endOfToday
                        );

        long cookedTotal =
                orderItemRepository
                        .countByAssignedLineChefIdAndCookingCompletedAtIsNotNull(
                                lineChef.getId()
                        );

        return new LineChefCookingStatsDTO(
                cookedToday,
                cookedTotal
        );
    }

    // =========================================================
    // AUDIT HELPERS
    // =========================================================

    private Map<String, Object> buildOrderItemAuditSnapshot(
            OrderItem item
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        if (item == null) {
            return snapshot;
        }

        snapshot.put(
                "orderItemId",
                item.getId()
        );

        snapshot.put(
                "orderId",
                item.getOrder() != null
                        ? item.getOrder().getId()
                        : null
        );

        snapshot.put(
                "itemName",
                item.getItemName()
        );

        snapshot.put(
                "quantity",
                item.getQuantity()
        );

        snapshot.put(
                "status",
                item.getStatus() != null
                        ? item.getStatus().name()
                        : null
        );

        snapshot.put(
                "assignedLineChefId",
                item.getAssignedLineChef() != null
                        ? item.getAssignedLineChef().getId()
                        : null
        );

        snapshot.put(
                "assignedLineChefName",
                buildStaffName(
                        item.getAssignedLineChef()
                )
        );

        snapshot.put(
                "cookingStartedAt",
                item.getCookingStartedAt()
        );

        snapshot.put(
                "cookingCompletedAt",
                item.getCookingCompletedAt()
        );

        snapshot.put(
                "kitchenNotes",
                item.getKitchenNotes()
        );

        return snapshot;
    }

    private Map<String, Object> buildOrderAuditSnapshot(
            Order order
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        if (order == null) {
            return snapshot;
        }

        snapshot.put(
                "orderId",
                order.getId()
        );

        snapshot.put(
                "orderNumber",
                order.getOrderNumber()
        );

        snapshot.put(
                "orderStatus",
                order.getStatus() != null
                        ? order.getStatus().name()
                        : null
        );

        snapshot.put(
                "orderType",
                order.getOrderType() != null
                        ? order.getOrderType().name()
                        : null
        );

        snapshot.put(
                "branchId",
                getOrderBranchId(order)
        );

        snapshot.put(
                "tableId",
                order.getTable() != null
                        ? order.getTable().getId()
                        : null
        );

        snapshot.put(
                "tableNumber",
                order.getTable() != null
                        ? order.getTable().getTableNumber()
                        : null
        );

        snapshot.put(
                "createdAt",
                order.getCreatedAt()
        );

        return snapshot;
    }

    private Map<String, Object> buildChefAttendanceAuditSnapshot(
            ChefAttendance attendance
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        if (attendance == null) {
            return snapshot;
        }

        snapshot.put(
                "attendanceId",
                attendance.getId()
        );

        snapshot.put(
                "workStatus",
                attendance.getWorkStatus() != null
                        ? attendance.getWorkStatus().name()
                        : null
        );

        return snapshot;
    }

    // =========================================================
    // COMMON HELPERS
    // =========================================================

    private Long getOrderBranchId(
            Order order
    ) {

        if (order == null
                || order.getBranch() == null) {

            return null;
        }

        return order.getBranch().getId();
    }

    private String buildStaffName(
            Staff staff
    ) {

        if (staff == null) {
            return null;
        }

        /*
         * Prefer User.fullName when available.
         */
        if (staff.getUser() != null
                && staff.getUser().getFullName() != null) {

            return staff
                    .getUser()
                    .getFullName();
        }

        /*
         * Fallback for older staff structure.
         */
        String firstName =
                staff.getFirstName() != null
                        ? staff.getFirstName()
                        : "";

        String lastName =
                staff.getLastName() != null
                        ? staff.getLastName()
                        : "";

        String fullName =
                (firstName + " " + lastName)
                        .trim();

        return fullName.isBlank()
                ? null
                : fullName;
    }
}