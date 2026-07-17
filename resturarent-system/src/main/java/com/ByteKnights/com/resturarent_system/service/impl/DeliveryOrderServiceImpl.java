package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryHistoryDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.DeliveryRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.DeliveryOrderService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

        private final DeliveryRepository deliveryRepository;
        private final OrderRepository orderRepository;
        private final StaffRepository staffRepository;
        private final WebSocketNotificationService webSocketNotificationService;
        private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryOrderDTO> getAssignedOrders(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Staff member not found for user ID: " + userId
                ));

        List<Delivery> assignments = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatus(
                staff.getId(),
                DeliveryStatus.ASSIGNED
        );

        return assignments.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryOrderDTO> getActiveOrder(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Staff member not found for user ID: " + userId
                ));

        List<Delivery> activeDeliveries = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatusIn(
                staff.getId(),
                Arrays.asList(DeliveryStatus.ACCEPTED, DeliveryStatus.OUT_FOR_DELIVERY)
        );

        if (activeDeliveries.isEmpty()) {
            return Optional.empty();
        }

        // Return the first one, assuming one active delivery at a time.
        return Optional.of(mapToDTO(activeDeliveries.get(0)));
    }

    private DeliveryOrderDTO mapToDTO(Delivery d) {
        return DeliveryOrderDTO.builder()
                .id(d.getOrder().getId())
                .orderNumber(d.getOrder().getOrderNumber() != null
                        ? d.getOrder().getOrderNumber()
                        : "ORD-" + d.getOrder().getId())
                .location(d.getOrder().getDeliveryAddress())
                                .deliveryAddress(d.getOrder().getDeliveryAddress())
                                .customerName(d.getOrder().getContactName())
                                .customerPhone(d.getOrder().getContactPhone())
                .paymentType("CASH ON DELIVERY")
                .amount(d.getOrder().getFinalAmount())
                .status(d.getDeliveryStatus().name())
                                .latitude(d.getOrder().getLatitude())
                                .longitude(d.getOrder().getLongitude())
                .build();
    }


    @Override
    @Transactional
    public void acceptOrder(Long orderId, Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Staff member not found for user ID: " + userId
                ));

        Delivery delivery = deliveryRepository.findByOrderIdAndDeliveryStaffId(orderId, staff.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found for order ID: " + orderId
                ));

        /*
         * Manual audit is used because this method changes delivery status.
         * oldValuesJson shows ASSIGNED and newValuesJson shows ACCEPTED.
         */
        Map<String, Object> oldValues = buildDeliveryAuditSnapshot(delivery);

        delivery.setDeliveryStatus(DeliveryStatus.ACCEPTED);

        Delivery savedDelivery = deliveryRepository.save(delivery);

        auditLogService.logCurrentUserAction(
                AuditModule.DELIVERY,
                AuditEventType.DELIVERY_ACCEPTED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.DELIVERY,
                savedDelivery.getId(),
                getDeliveryBranchId(savedDelivery),
                "Delivery order accepted successfully",
                oldValues,
                buildDeliveryAuditSnapshot(savedDelivery)
        );
    }

    @Override
    @Transactional
    public void rejectOrder(Long orderId, Long userId, String reason) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Staff member not found for user ID: " + userId
                ));

        Delivery delivery = deliveryRepository.findByOrderIdAndDeliveryStaffId(orderId, staff.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found for order ID: " + orderId
                ));

        /*
         * Manual audit is used because this method changes delivery status
         * and saves a rejection/cancel reason.
         */
        Map<String, Object> oldValues = buildDeliveryAuditSnapshot(delivery);

        delivery.setDeliveryStatus(DeliveryStatus.CANCELLED);
        delivery.setCancelledAt(LocalDateTime.now());
        delivery.setCancelledReason(reason);

        Delivery savedDelivery = deliveryRepository.save(delivery);

        auditLogService.logCurrentUserAction(
                AuditModule.DELIVERY,
                AuditEventType.DELIVERY_REJECTED,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.DELIVERY,
                savedDelivery.getId(),
                getDeliveryBranchId(savedDelivery),
                "Delivery order rejected successfully",
                oldValues,
                buildDeliveryAuditSnapshot(savedDelivery)
        );
    }

    @Override
    @Transactional
    public void updateStatus(Long orderId, Long userId, DeliveryStatus status) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Staff member not found for user ID: " + userId
                ));

        Delivery delivery = deliveryRepository.findByOrderIdAndDeliveryStaffId(orderId, staff.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found for order ID: " + orderId
                ));

        /*
         * Manual audit is used because this method may update both delivery status
         * and the parent order status when the delivery is completed.
         */
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("delivery", buildDeliveryAuditSnapshot(delivery));
        oldValues.put("order", buildDeliveryOrderAuditSnapshot(delivery.getOrder()));

        delivery.setDeliveryStatus(status);
        if (status == DeliveryStatus.CANCELLED) {
            delivery.setCancelledAt(LocalDateTime.now());
        }

        if (status == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());

            /*
             * Explicitly save the Order status to SERVED via OrderRepository,
             * because the Delivery -> Order relationship has no cascade.
             */
            delivery.getOrder().setStatus(OrderStatus.SERVED);
            orderRepository.save(delivery.getOrder());
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("delivery", buildDeliveryAuditSnapshot(savedDelivery));
        newValues.put("order", buildDeliveryOrderAuditSnapshot(savedDelivery.getOrder()));

        auditLogService.logCurrentUserAction(
                AuditModule.DELIVERY,
                AuditEventType.DELIVERY_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                status == DeliveryStatus.DELIVERED ? AuditSeverity.INFO : AuditSeverity.INFO,
                AuditTargetType.DELIVERY,
                savedDelivery.getId(),
                getDeliveryBranchId(savedDelivery),
                "Delivery status updated successfully",
                oldValues,
                newValues
        );
    }

    /*
     * Builds safe audit JSON for delivery changes.
     * We store only useful fields instead of saving the full entity object.
     */
    private Map<String, Object> buildDeliveryAuditSnapshot(Delivery delivery) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (delivery == null) {
            return snapshot;
        }

        Order order = delivery.getOrder();
        Staff deliveryStaff = delivery.getDeliveryStaff();

        snapshot.put("deliveryId", delivery.getId());

        snapshot.put("orderId", order != null ? order.getId() : null);
        snapshot.put("orderNumber", order != null ? order.getOrderNumber() : null);
        snapshot.put("branchId", getDeliveryBranchId(delivery));

        snapshot.put("deliveryStatus",
                delivery.getDeliveryStatus() != null
                        ? delivery.getDeliveryStatus().name()
                        : null);

        snapshot.put("deliveryAddress", order != null ? order.getDeliveryAddress() : null);
        snapshot.put("cancelledReason", delivery.getCancelledReason());
        snapshot.put("deliveredAt", delivery.getDeliveredAt());

        snapshot.put("deliveryStaffId", deliveryStaff != null ? deliveryStaff.getId() : null);
        snapshot.put("deliveryStaffUserId",
                deliveryStaff != null && deliveryStaff.getUser() != null
                        ? deliveryStaff.getUser().getId()
                        : null);
        snapshot.put("deliveryStaffName",
                deliveryStaff != null && deliveryStaff.getUser() != null
                        ? deliveryStaff.getUser().getFullName()
                        : null);

        return snapshot;
    }

    /*
     * Builds safe audit JSON for the parent order.
     * This is useful when delivery completion also changes order status to SERVED.
     */
    private Map<String, Object> buildDeliveryOrderAuditSnapshot(Order order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (order == null) {
            return snapshot;
        }

        snapshot.put("orderId", order.getId());
        snapshot.put("orderNumber", order.getOrderNumber());
        snapshot.put("branchId", order.getBranch() != null ? order.getBranch().getId() : null);
        snapshot.put("branchName", order.getBranch() != null ? order.getBranch().getName() : null);

        snapshot.put("orderStatus", order.getStatus() != null ? order.getStatus().name() : null);
        snapshot.put("paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        snapshot.put("orderType", order.getOrderType() != null ? order.getOrderType().name() : null);

        snapshot.put("deliveryAddress", order.getDeliveryAddress());
        snapshot.put("finalAmount", order.getFinalAmount());
        snapshot.put("createdAt", order.getCreatedAt());
        snapshot.put("statusUpdatedAt", order.getStatusUpdatedAt());

        return snapshot;
    }

    /*
     * Gets branch ID for audit branch filtering.
     */
    private Long getDeliveryBranchId(Delivery delivery) {
        if (delivery == null || delivery.getOrder() == null || delivery.getOrder().getBranch() == null) {
            return null;
        }

        return delivery.getOrder().getBranch().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryHistoryDTO> getDeliveryHistory(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Staff member not found for user ID: " + userId
                ));

        // Fetches the 50 most recent DELIVERED/CANCELLED entries for this driver.
        // JOIN FETCH d.order loads the Order in the same SQL query (no lazy-load N+1 per row).
        // DB-level ORDER BY replaces the previous in-memory sort.
        // PageRequest.of(0, 50) prevents unbounded memory growth for long-serving drivers.
        List<Delivery> history = deliveryRepository.findHistoryByStaffIdPaged(
                staff.getId(),
                Arrays.asList(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED),
                PageRequest.of(0, 50)
        );

        return history.stream()
                .map(d -> {
                    Order order = d.getOrder();
                    String customerName = null;
                    String customerPhone = null;
                    if (order != null && order.getCustomer() != null && order.getCustomer().getUser() != null) {
                        customerName = order.getCustomer().getUser().getFullName();
                        customerPhone = order.getCustomer().getUser().getPhone();
                    }
                    return DeliveryHistoryDTO.builder()
                            .id(d.getId())
                            .orderId(order != null ? order.getId() : null)
                            .orderNumber(order != null ? order.getOrderNumber() : null)
                            .customerName(customerName)
                            .customerPhone(customerPhone)
                            .deliveryAddress(order != null ? order.getDeliveryAddress() : null)
                            .amount(order != null ? order.getFinalAmount() : null)
                            .status(d.getDeliveryStatus() != null ? d.getDeliveryStatus().name() : null)
                            .completedAt(d.getDeliveredAt())
                            .cancelledReason(d.getCancelledReason())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
