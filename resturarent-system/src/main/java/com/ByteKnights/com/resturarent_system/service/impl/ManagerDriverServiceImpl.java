package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerDriverSummaryDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.DeliveryRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.ManagerDriverService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerDriverServiceImpl implements ManagerDriverService {

        private final OrderRepository orderRepository;
        private final StaffRepository staffRepository;
        private final DeliveryRepository deliveryRepository;
        private final WebSocketNotificationService webSocketNotificationService;
        private final AuditLogService auditLogService;

        @Override
        @Transactional(readOnly = true)
        public ManagerDriverSummaryDTO getDriverSummary(Long targetBranchId, Long userId) {
                Long finalBranchId = resolveBranchId(targetBranchId, userId);

                // 1. Fetch all Riders for the branch
                List<Staff> riders = staffRepository.findByBranchIdAndUserRoleName(finalBranchId, "DELIVERY");

                // 2. Fetch Dispatchable Orders, completed by kitchen and ready for dispatch
                List<Order> allCompleted = orderRepository.findByBranchIdAndOrderTypeAndStatus(
                                finalBranchId,
                                OrderType.ONLINE_DELIVERY,
                                OrderStatus.COMPLETED);

                // Filter out orders that already have a delivery assignment
                List<Order> dispatchableOrders = allCompleted.stream()
                                .filter(order -> {
                                        java.util.Optional<Delivery> existing = deliveryRepository.findByOrder(order);
                                        return existing.isEmpty();
                                })
                                .collect(Collectors.toList());

                // 3. Map Riders and calculate metrics
                List<DeliveryStatus> activeStatuses = Arrays.asList(
                                DeliveryStatus.ASSIGNED,
                                DeliveryStatus.ACCEPTED,
                                DeliveryStatus.OUT_FOR_DELIVERY);

                int driversOnline = 0;
                int available = 0;
                int busy = 0;

                List<ManagerDriverSummaryDTO.DriverStatusDTO> driverDTOs = riders.stream()
                                .map(rider -> {
                                        // Find active delivery
                                        List<Delivery> activeDeliveries = deliveryRepository
                                                        .findByDeliveryStaffIdAndDeliveryStatusIn(
                                                                        rider.getId(),
                                                                        activeStatuses);

                                        boolean isBusy = !activeDeliveries.isEmpty();

                                        String status = "INACTIVE";

                                        if (rider.getEmploymentStatus() == EmploymentStatus.ACTIVE) {
                                                status = rider.isOnline()
                                                                ? (isBusy ? activeDeliveries.get(0).getDeliveryStatus()
                                                                                .name() : "Available")
                                                                : "Offline";
                                        }

                                        ManagerDriverSummaryDTO.CurrentTaskDTO currentTask = null;

                                        if (isBusy) {
                                                Delivery current = activeDeliveries.get(0);

                                                java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter
                                                                .ofPattern("HH:mm");

                                                currentTask = ManagerDriverSummaryDTO.CurrentTaskDTO.builder()
                                                                .orderId(current.getOrder().getOrderNumber() != null
                                                                                ? current.getOrder().getOrderNumber()
                                                                                : "ORD-" + current.getOrder().getId())
                                                                .assignedTime(current.getAssignedAt() != null
                                                                                ? current.getAssignedAt()
                                                                                                .format(timeFormatter)
                                                                                : "--:--")
                                                                .build();
                                        }

                                        return ManagerDriverSummaryDTO.DriverStatusDTO.builder()
                                                        .id(rider.getId())
                                                        .name(rider.getFirstName() + " " + rider.getLastName())
                                                        .avatar(null)
                                                        .rating(4.5)
                                                        .status(status)
                                                        .currentTask(currentTask)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // Update metrics
                for (ManagerDriverSummaryDTO.DriverStatusDTO d : driverDTOs) {
                        if (!"INACTIVE".equals(d.getStatus()) && !"Offline".equals(d.getStatus())) {
                                driversOnline++;

                                if ("Available".equals(d.getStatus())) {
                                        available++;
                                } else {
                                        busy++;
                                }
                        }
                }

                // 4. Map Dispatch Orders
                List<ManagerDriverSummaryDTO.DispatchOrderDTO> orderDTOs = dispatchableOrders.stream()
                                .map(order -> ManagerDriverSummaryDTO.DispatchOrderDTO.builder()
                                                .id(order.getOrderNumber() != null
                                                                ? order.getOrderNumber()
                                                                : "ORD-" + order.getId())
                                                .orderId(order.getId())
                                                .status("Ready for Pickup")
                                                .customerName(order.getContactName() != null
                                                                ? order.getContactName()
                                                                : "Customer")
                                                .zone(order.getDeliveryAddress() != null
                                                                ? order.getDeliveryAddress()
                                                                : "Pickup Order")
                                                .distance("N/A")
                                                .build())
                                .collect(Collectors.toList());

                // 5. Delivery History, DELIVERED and CANCELLED
                java.time.format.DateTimeFormatter historyFormatter = java.time.format.DateTimeFormatter
                                .ofPattern("MMM dd, yyyy HH:mm");

                List<Delivery> historyDeliveries = deliveryRepository.findDeliveryHistoryByBranchId(
                                finalBranchId,
                                Arrays.asList(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED));

                List<ManagerDriverSummaryDTO.DeliveryHistoryDTO> historyDTOs = historyDeliveries.stream()
                                .map(d -> ManagerDriverSummaryDTO.DeliveryHistoryDTO.builder()
                                                .orderId(d.getOrder().getOrderNumber() != null
                                                                ? d.getOrder().getOrderNumber()
                                                                : "ORD-" + d.getOrder().getId())
                                                .deliveryStatus(d.getDeliveryStatus().name())
                                                .driverName(d.getDeliveryStaff().getFirstName() + " "
                                                                + d.getDeliveryStaff().getLastName())
                                                .completedAt(d.getDeliveredAt() != null
                                                                ? d.getDeliveredAt().format(historyFormatter)
                                                                : "N/A")
                                                .build())
                                .collect(Collectors.toList());

                return ManagerDriverSummaryDTO.builder()
                                .driversOnline(driversOnline)
                                .available(available)
                                .busy(busy)
                                .pendingDispatch(orderDTOs.size())
                                .dispatchOrders(orderDTOs)
                                .drivers(driverDTOs)
                                .deliveryHistory(historyDTOs)
                                .build();
        }

        @Override
        @Transactional
        public void assignDriver(Long orderId, Long driverId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Order not found with ID: " + orderId));

                Staff rider = staffRepository.findById(driverId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Rider not found with ID: " + driverId));

                if (!"DELIVERY".equals(rider.getUser().getRole().getName())) {
                        throw new IllegalArgumentException("Staff member is not a delivery person");
                }

                if (rider.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
                        throw new IllegalArgumentException("Rider is not active");
                }

                /*
                 * Branch safety check:
                 * The selected rider must belong to the same branch as the delivery order.
                 */
                Long orderBranchId = getOrderBranchId(order);
                Long riderBranchId = getStaffBranchId(rider);

                if (orderBranchId == null || riderBranchId == null || !orderBranchId.equals(riderBranchId)) {
                        throw new IllegalArgumentException("Rider and order must belong to the same branch");
                }

                /*
                 * Capture old values before creating the delivery assignment and updating
                 * the order status.
                 */
                Map<String, Object> oldValues = new LinkedHashMap<>();
                oldValues.put("order", buildOrderAuditSnapshot(order));
                oldValues.put("driver", buildDriverAuditSnapshot(rider));
                oldValues.put("delivery", null);

                // Create Delivery record
                Delivery delivery = Delivery.builder()
                                .order(order)
                                .deliveryStaff(rider)
                                .deliveryStatus(DeliveryStatus.ASSIGNED)
                                .assignedAt(LocalDateTime.now())
                                .build();

                Delivery savedDelivery = deliveryRepository.save(delivery);

                // Update Order status
                order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
                Order savedOrder = orderRepository.save(order);
                webSocketNotificationService.broadcastOrderStatusUpdate(order.getId(), order.getStatus().name());

                /*
                 * Capture new values after assignment.
                 */
                Map<String, Object> newValues = new LinkedHashMap<>();
                newValues.put("order", buildOrderAuditSnapshot(savedOrder));
                newValues.put("driver", buildDriverAuditSnapshot(rider));
                newValues.put("delivery", buildDeliveryAuditSnapshot(savedDelivery));

                auditLogService.logCurrentUserAction(
                                AuditModule.DELIVERY,
                                AuditEventType.DRIVER_ASSIGNED,
                                AuditStatus.SUCCESS,
                                AuditSeverity.INFO,
                                AuditTargetType.DELIVERY,
                                savedDelivery.getId(),
                                orderBranchId,
                                "Driver assigned to delivery order successfully",
                                oldValues,
                                newValues);
        }

        private Long resolveBranchId(Long targetBranchId, Long userId) {
                if (targetBranchId != null) {
                        return targetBranchId;
                }

                Staff staff = staffRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User is not assigned to any branch as staff"));

                if (staff.getBranch() == null) {
                        throw new IllegalArgumentException("Staff member is not assigned to a branch");
                }

                return staff.getBranch().getId();
        }

        /*
         * Builds safe audit JSON for the delivery assignment.
         */
        private Map<String, Object> buildDeliveryAuditSnapshot(Delivery delivery) {
                Map<String, Object> snapshot = new LinkedHashMap<>();

                if (delivery == null) {
                        return snapshot;
                }

                Order order = delivery.getOrder();
                Staff driver = delivery.getDeliveryStaff();

                snapshot.put("deliveryId", delivery.getId());

                snapshot.put("orderId", order != null ? order.getId() : null);
                snapshot.put("orderNumber", order != null ? order.getOrderNumber() : null);
                snapshot.put("branchId", order != null && order.getBranch() != null ? order.getBranch().getId() : null);

                snapshot.put("deliveryStatus",
                                delivery.getDeliveryStatus() != null
                                                ? delivery.getDeliveryStatus().name()
                                                : null);

                snapshot.put("assignedAt", delivery.getAssignedAt());
                snapshot.put("deliveredAt", delivery.getDeliveredAt());
                snapshot.put("cancelledReason", delivery.getCancelledReason());

                snapshot.put("driverStaffId", driver != null ? driver.getId() : null);
                snapshot.put("driverUserId",
                                driver != null && driver.getUser() != null
                                                ? driver.getUser().getId()
                                                : null);
                snapshot.put("driverName",
                                driver != null && driver.getUser() != null
                                                ? driver.getUser().getFullName()
                                                : null);

                return snapshot;
        }

        /*
         * Builds safe audit JSON for the order affected by driver assignment.
         */
        private Map<String, Object> buildOrderAuditSnapshot(Order order) {
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
                snapshot.put("paymentStatus",
                                order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);

                snapshot.put("contactName", order.getContactName());
                snapshot.put("contactPhone", order.getContactPhone());
                snapshot.put("deliveryAddress", order.getDeliveryAddress());
                snapshot.put("finalAmount", order.getFinalAmount());

                snapshot.put("createdAt", order.getCreatedAt());
                snapshot.put("statusUpdatedAt", order.getStatusUpdatedAt());

                return snapshot;
        }

        /*
         * Builds safe audit JSON for the assigned driver.
         */
        private Map<String, Object> buildDriverAuditSnapshot(Staff driver) {
                Map<String, Object> snapshot = new LinkedHashMap<>();

                if (driver == null) {
                        return snapshot;
                }

                snapshot.put("driverStaffId", driver.getId());
                snapshot.put("driverUserId",
                                driver.getUser() != null ? driver.getUser().getId() : null);
                snapshot.put("driverName",
                                driver.getUser() != null ? driver.getUser().getFullName() : null);
                snapshot.put("driverEmail",
                                driver.getUser() != null ? driver.getUser().getEmail() : null);

                snapshot.put("role",
                                driver.getUser() != null && driver.getUser().getRole() != null
                                                ? driver.getUser().getRole().getName()
                                                : null);

                snapshot.put("employmentStatus",
                                driver.getEmploymentStatus() != null
                                                ? driver.getEmploymentStatus().name()
                                                : null);

                snapshot.put("online", driver.isOnline());
                snapshot.put("branchId", getStaffBranchId(driver));
                snapshot.put("branchName", driver.getBranch() != null ? driver.getBranch().getName() : null);

                return snapshot;
        }

        /*
         * Gets order branch ID for audit branch filtering.
         */
        private Long getOrderBranchId(Order order) {
                if (order == null || order.getBranch() == null) {
                        return null;
                }
                return order.getBranch().getId();
        }

        /*
         * Gets staff branch ID.
         */
        private Long getStaffBranchId(Staff staff) {
                if (staff == null || staff.getBranch() == null) {
                        return null;
                }

                return staff.getBranch().getId();
        }
}
