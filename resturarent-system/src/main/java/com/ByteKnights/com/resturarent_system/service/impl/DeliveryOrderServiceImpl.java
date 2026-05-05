package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.entity.Delivery;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.repository.DeliveryRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.DeliveryOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;

@Service
@RequiredArgsConstructor
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

        private final DeliveryRepository deliveryRepository;
        private final OrderRepository orderRepository;
        private final StaffRepository staffRepository;

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

                // Query the database for deliveries assigned to this staff member that are still pending acceptance
                List<Delivery> assignments = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatus(staff.getId(),
                                DeliveryStatus.ASSIGNED);

                // Convert the raw Delivery entities into DTOs to send back to the frontend
                return assignments.stream().map(d -> mapToDTO(d)).collect(Collectors.toList());
        }

        /**
         * Fetches the single active delivery task for a specific driver.
         * An active task is one that the driver has accepted or is currently out delivering.
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

                // Return the first active delivery found (assuming drivers handle one delivery at a time)
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

                // Mark the delivery as cancelled and save the driver's provided reason
                delivery.setDeliveryStatus(DeliveryStatus.CANCELLED);
                delivery.setCancelledReason(reason);
                deliveryRepository.save(delivery);
        }

        /**
         * Updates the real-time status of a delivery (e.g., changing from ACCEPTED to OUT_FOR_DELIVERY).
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

                delivery.setDeliveryStatus(status);
                
                // Special case: If the delivery is finalized, sync the master Order table
                if (status == DeliveryStatus.DELIVERED) {
                        delivery.setDeliveredAt(LocalDateTime.now());
                        // Explicitly save the Order status to SERVED via OrderRepository,
                        // because the Delivery -> Order relationship has no cascade.
                        delivery.getOrder().setStatus(OrderStatus.SERVED);
                        orderRepository.save(delivery.getOrder());
                }

                deliveryRepository.save(delivery);
        }
}
