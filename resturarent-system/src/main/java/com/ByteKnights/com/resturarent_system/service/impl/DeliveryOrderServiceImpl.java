package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryHistoryDTO;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Delivery;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.repository.DeliveryRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.DeliveryOrderService;
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

@Service
@RequiredArgsConstructor
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

        private final DeliveryRepository deliveryRepository;
        private final OrderRepository orderRepository;
        private final StaffRepository staffRepository;
        private final AuditLogService auditLogService;

        /**
         * Fetches all delivery tasks that are newly assigned to a specific driver.
         * This queries for deliveries with the 'ASSIGNED' status only.
         */
        @Override
        @Transactional(readOnly = true)
        public List<DeliveryOrderDTO> getAssignedOrders(Long userId) {
                // Find the staff record linked to the logged-in user
                Staff staff = staffRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Staff member not found for user ID: " + userId));

                // Query the database for deliveries assigned to this staff member that are
                // still pending acceptance
                List<Delivery> assignments = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatus(staff.getId(),
                                DeliveryStatus.ASSIGNED);

                // Convert the raw Delivery entities into DTOs to send back to the frontend
                return assignments.stream().map(d -> mapToDTO(d)).collect(Collectors.toList());
        }

        /**
         * Fetches the single active delivery task for a specific driver.
         * An active task is one that the driver has accepted or is currently out
         * delivering.
         */
        @Override
        @Transactional(readOnly = true)
        public Optional<DeliveryOrderDTO> getActiveOrder(Long userId) {
                Staff staff = staffRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Staff member not found for user ID: " + userId));

                // Find deliveries for this staff member that are currently in progress
                List<Delivery> activeDeliveries = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatusIn(
                                staff.getId(),
                                Arrays.asList(DeliveryStatus.ACCEPTED, DeliveryStatus.OUT_FOR_DELIVERY));

                // If the driver is not currently delivering anything, return empty
                if (activeDeliveries.isEmpty()) {
                        return Optional.empty();
                }

                // Return the first active delivery found (assuming drivers handle one delivery
                // at a time)
                return Optional.of(mapToDTO(activeDeliveries.get(0)));
        }

        /**
         * Helper method to map a raw Delivery entity to a safe DeliveryOrderDTO
         * to expose only necessary data to the frontend driver application.
         */
        private DeliveryOrderDTO mapToDTO(Delivery d) {
                return DeliveryOrderDTO.builder()
                                .id(d.getOrder().getId())
                                .orderNumber(d.getOrder().getOrderNumber() != null ? d.getOrder().getOrderNumber()
                                                : "ORD-" + d.getOrder().getId())
                                .location(d.getOrder().getDeliveryAddress())
                                .paymentType("CASH ON DELIVERY")
                                .amount(d.getOrder().getFinalAmount())
                                .status(d.getDeliveryStatus().name())
                                .build();
        }

        /**
         * Accepts a delivery assignment, moving it from ASSIGNED to ACCEPTED.
         */
        @Override
        @Transactional
        public void acceptOrder(Long orderId, Long userId) {
                Staff staff = staffRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Staff member not found for user ID: " + userId));

                // Ensure the order is actually assigned to this specific driver
                Delivery delivery = deliveryRepository.findByOrderIdAndDeliveryStaffId(orderId, staff.getId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Assignment not found for order ID: " + orderId));

                // Update the delivery lifecycle status
                delivery.setDeliveryStatus(DeliveryStatus.ACCEPTED);
                deliveryRepository.save(delivery);
        }

        /**
         * Rejects a delivery assignment, adding a reason for the cancellation.
         */
        @Override
        @Transactional
        public void rejectOrder(Long orderId, Long userId, String reason) {
                Staff staff = staffRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Staff member not found for user ID: " + userId));

                Delivery delivery = deliveryRepository.findByOrderIdAndDeliveryStaffId(orderId, staff.getId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Assignment not found for order ID: " + orderId));

                /*
                 * Manual audit is used because this method changes delivery status
                 * and saves a rejection/cancel reason.
                 */
                Map<String, Object> oldValues = buildDeliveryAuditSnapshot(delivery);

                // Mark the delivery as cancelled and save the driver's provided reason
                delivery.setDeliveryStatus(DeliveryStatus.CANCELLED);
                delivery.setCancelledReason(reason);
                deliveryRepository.save(delivery);
        }

        /**
         * Updates the real-time status of a delivery (e.g., changing from ACCEPTED to
         * OUT_FOR_DELIVERY).
         * If marked as DELIVERED, it also updates the master Order status to SERVED.
         */
        @Override
        @Transactional
        public void updateStatus(Long orderId, Long userId, DeliveryStatus status) {
                Staff staff = staffRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Staff member not found for user ID: " + userId));

                Delivery delivery = deliveryRepository.findByOrderIdAndDeliveryStaffId(orderId, staff.getId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Assignment not found for order ID: " + orderId));

                /*
                 * Manual audit is used because this method may update both delivery status
                 * and the parent order status when the delivery is completed.
                 */
                Map<String, Object> oldValues = new LinkedHashMap<>();
                oldValues.put("delivery", buildDeliveryAuditSnapshot(delivery));
                oldValues.put("order", buildDeliveryOrderAuditSnapshot(delivery.getOrder()));

                delivery.setDeliveryStatus(status);

                // Special case: If the delivery is finalized, sync the master Order table
                if (status == DeliveryStatus.DELIVERED) {
                        delivery.setDeliveredAt(LocalDateTime.now());
                        // Explicitly save the Order status to SERVED via OrderRepository,
                        // because the Delivery -> Order relationship has no cascade.
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
                                newValues);
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
        snapshot.put("id", delivery.getId());
        snapshot.put("status", delivery.getDeliveryStatus() != null ? delivery.getDeliveryStatus().name() : null);
        snapshot.put("assigned_at", delivery.getAssignedAt());
        snapshot.put("delivered_at", delivery.getDeliveredAt());
        snapshot.put("cancelled_reason", delivery.getCancelledReason());
        return snapshot;
    }

    private Map<String, Object> buildDeliveryOrderAuditSnapshot(Order order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (order == null) {
            return snapshot;
        }
        snapshot.put("id", order.getId());
        snapshot.put("order_number", order.getOrderNumber());
        snapshot.put("status", order.getStatus() != null ? order.getStatus().name() : null);
        snapshot.put("amount", order.getFinalAmount());
        return snapshot;
    }

    private Long getDeliveryBranchId(Delivery delivery) {
        if (delivery != null && delivery.getOrder() != null && delivery.getOrder().getBranch() != null) {
            return delivery.getOrder().getBranch().getId();
        }
        return null;
    }

    /**
     * Retrieves historical deliveries (DELIVERED or CANCELLED) for a specific
     * driver.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DeliveryHistoryDTO> getDeliveryHistory(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Staff member not found for user ID: " + userId));

        List<Delivery> history = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatusIn(
                staff.getId(),
                Arrays.asList(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED));

        // Sort descending by completion time (deliveredAt or assignedAt as fallback)
        return history.stream().map(d -> {
            LocalDateTime completedAt = d.getDeliveryStatus() == DeliveryStatus.DELIVERED
                    ? d.getDeliveredAt()
                    : (d.getDeliveredAt() != null ? d.getDeliveredAt() : d.getAssignedAt());

            return DeliveryHistoryDTO.builder()
                    .id(d.getId())
                    .orderId(d.getOrder().getId())
                    .orderNumber(d.getOrder().getOrderNumber() != null
                            ? d.getOrder().getOrderNumber()
                            : "ORD-" + d.getOrder().getId())
                    .customerName(d.getOrder().getContactName())
                    .customerPhone(d.getOrder().getContactPhone())
                    .deliveryAddress(d.getOrder().getDeliveryAddress())
                    .amount(d.getOrder().getFinalAmount())
                    .status(d.getDeliveryStatus().name())
                    .completedAt(completedAt)
                    .cancelledReason(d.getCancelledReason())
                    .build();
        })
        .sorted((a, b) -> {
            if (a.getCompletedAt() == null && b.getCompletedAt() == null)
                return 0;
            if (a.getCompletedAt() == null)
                return 1;
            if (b.getCompletedAt() == null)
                return -1;
            return b.getCompletedAt().compareTo(a.getCompletedAt());
        })
        .collect(Collectors.toList());
    }
}
