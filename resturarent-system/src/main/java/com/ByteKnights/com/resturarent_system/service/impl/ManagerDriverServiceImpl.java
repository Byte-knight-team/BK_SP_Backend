package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerDriverSummaryDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.DeliveryRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.ManagerDriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagerDriverServiceImpl implements ManagerDriverService {

        private final OrderRepository orderRepository;
        private final StaffRepository staffRepository;
        private final DeliveryRepository deliveryRepository;

        /**
         * Compiles the data required for the Manager's Delivery Dashboard.
         * Includes online drivers, dispatchable orders, and historical deliveries.
         *
         * @param targetBranchId Optional branch ID filter.
         * @param userId The ID of the currently authenticated manager.
         * @return A comprehensive summary of driver activity and dispatch status.
         */
        @Override
        @Transactional(readOnly = true)
        public ManagerDriverSummaryDTO getDriverSummary(Long targetBranchId, Long userId) {
                Long finalBranchId = resolveBranchId(targetBranchId, userId);

                // 1. Fetch all Riders for the branch
                List<Staff> riders = staffRepository.findByBranchIdAndUserRoleName(finalBranchId, "DELIVERY");

                // 2. Fetch Dispatchable Orders (COMPLETED by kitchen, ready for dispatch)
                List<Order> allCompleted = orderRepository.findByBranchIdAndOrderTypeAndStatus(
                                finalBranchId, OrderType.ONLINE_DELIVERY, OrderStatus.COMPLETED);

                // Filter out orders that already have a delivery assignment
                List<Order> dispatchableOrders = allCompleted.stream()
                                .filter(order -> {
                                        java.util.Optional<Delivery> existing = deliveryRepository.findByOrder(order);
                                        return existing.isEmpty();
                                })
                                .collect(Collectors.toList());

                // 3. Map Riders and calculate metrics
                List<DeliveryStatus> activeStatuses = Arrays.asList(
                                DeliveryStatus.ASSIGNED, DeliveryStatus.ACCEPTED, DeliveryStatus.OUT_FOR_DELIVERY);

                int driversOnline = 0;
                int available = 0;
                int busy = 0;

                List<ManagerDriverSummaryDTO.DriverStatusDTO> driverDTOs = riders.stream().map(rider -> {
                        // Find active delivery
                        List<Delivery> activeDeliveries = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatusIn(
                                        rider.getId(),
                                        activeStatuses);
                        boolean isBusy = !activeDeliveries.isEmpty();

                        String status = "INACTIVE";
                        if (rider.getEmploymentStatus() == EmploymentStatus.ACTIVE) {
                                status = rider.isOnline()
                                                ? (isBusy ? activeDeliveries.get(0).getDeliveryStatus().name()
                                                                : "Available")
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
                                                                ? current.getAssignedAt().format(timeFormatter)
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
                }).collect(Collectors.toList());

                // Update metrics
                for (ManagerDriverSummaryDTO.DriverStatusDTO d : driverDTOs) {
                        if (!"INACTIVE".equals(d.getStatus()) && !"Offline".equals(d.getStatus())) {
                                driversOnline++;
                                if ("Available".equals(d.getStatus()))
                                        available++;
                                else
                                        busy++;
                        }
                }

                // 4. Map Dispatch Orders
                List<ManagerDriverSummaryDTO.DispatchOrderDTO> orderDTOs = dispatchableOrders.stream()
                                .map(order -> ManagerDriverSummaryDTO.DispatchOrderDTO.builder()
                                                .id(order.getOrderNumber() != null ? order.getOrderNumber()
                                                                : "ORD-" + order.getId())
                                                .orderId(order.getId())
                                                .status("Ready for Pickup")
                                                .customerName(order.getContactName() != null ? order.getContactName()
                                                                : "Customer")
                                                .zone(order.getDeliveryAddress() != null ? order.getDeliveryAddress()
                                                                : "Pickup Order")
                                                .distance("N/A")
                                                .build())
                                .collect(Collectors.toList());

                // 5. Delivery History (DELIVERED and CANCELLED)
                java.time.format.DateTimeFormatter historyFormatter = java.time.format.DateTimeFormatter
                                .ofPattern("MMM dd, yyyy HH:mm");
                List<Delivery> historyDeliveries = deliveryRepository.findDeliveryHistoryByBranchId(
                                finalBranchId, Arrays.asList(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED));

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

        /**
         * Manual assignment of a delivery task to a specific driver by the Manager.
         * Generates the Delivery record and updates the core Order status.
         *
         * @param orderId ID of the order being assigned.
         * @param driverId ID of the driver receiving the assignment.
         */
        @Override
        @org.springframework.transaction.annotation.Transactional
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

                // Create Delivery record
                Delivery delivery = Delivery.builder()
                                .order(order)
                                .deliveryStaff(rider)
                                .deliveryStatus(DeliveryStatus.ASSIGNED)
                                .assignedAt(LocalDateTime.now())
                                .build();

                deliveryRepository.save(delivery);

                // Update Order status
                order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
                orderRepository.save(order);
        }

        /**
         * Helper method to determine the correct branch context.
         */
        private Long resolveBranchId(Long targetBranchId, Long userId) {
                if (targetBranchId != null)
                        return targetBranchId;
                Staff staff = staffRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User is not assigned to any branch as staff"));
                if (staff.getBranch() == null) {
                        throw new IllegalArgumentException("Staff member is not assigned to a branch");
                }
                return staff.getBranch().getId();
        }
}
