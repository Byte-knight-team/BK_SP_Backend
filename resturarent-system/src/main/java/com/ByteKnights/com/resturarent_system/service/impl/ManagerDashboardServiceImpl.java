package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerDashboardSummaryDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.ManagerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerDashboardServiceImpl implements ManagerDashboardService {

    private final OrderRepository orderRepository;
    private final StaffRepository staffRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    public ManagerDashboardSummaryDTO getDashboardSummary(Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        // 1. Revenue
        BigDecimal revenue = orderRepository.sumFinalAmountByBranchIdAndStatusAndCreatedAtBetween(
                finalBranchId, OrderStatus.COMPLETED, startOfDay, endOfDay);

        // 2. Active Orders
        List<OrderStatus> activeStatuses = Arrays.asList(
                OrderStatus.PLACED, OrderStatus.APPROVED, OrderStatus.PENDING, 
                OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.ON_HOLD);
        int activeOrders = (int) orderRepository.countByBranchIdAndStatusIn(finalBranchId, activeStatuses);

        // 3. Pending Deliveries
        List<OrderStatus> deliveryStatuses = Arrays.asList(OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY);
        // We use countByBranchIdAndStatusIn because OrderRepository doesn't have orderType and status combined.
        // Let's do a manual fetch or add a query if needed. 
        // For now, let's fetch orders for today with delivery statuses and filter by ONLINE
        int pendingDeliveries = 0;
        List<Order> deliveries = orderRepository.findByStatusAndStatusUpdatedAtAfter(OrderStatus.READY, startOfDay, org.springframework.data.domain.Sort.by("createdAt"));
        for (Order o : deliveries) {
            if (o.getBranch().getId().equals(finalBranchId) && o.getOrderType() == OrderType.ONLINE_DELIVERY) {
                pendingDeliveries++;
            }
        }
        List<Order> outDeliveries = orderRepository.findByStatusAndStatusUpdatedAtAfter(OrderStatus.OUT_FOR_DELIVERY, startOfDay, org.springframework.data.domain.Sort.by("createdAt"));
        for (Order o : outDeliveries) {
            if (o.getBranch().getId().equals(finalBranchId) && o.getOrderType() == OrderType.ONLINE_DELIVERY) {
                pendingDeliveries++;
            }
        }

        // 4. Low Stock Alerts
        int lowStockAlerts = (int) inventoryItemRepository.countLowStockByBranchId(finalBranchId);

        // 5. Sales Target (Mocked Goal for now, dynamic current)
        ManagerDashboardSummaryDTO.ManagerSalesTargetDTO salesTarget = ManagerDashboardSummaryDTO.ManagerSalesTargetDTO.builder()
                .current(revenue)
                .goal(new BigDecimal("50000.00")) // Hardcoded target
                .build();

        // 6. Order Distribution
        int dineIn = (int) orderRepository.countByBranchIdAndOrderTypeAndCreatedAtBetween(finalBranchId, OrderType.QR, startOfDay, endOfDay);
        int onlineDelivery = (int) orderRepository.countByBranchIdAndOrderTypeAndCreatedAtBetween(finalBranchId, OrderType.ONLINE_DELIVERY, startOfDay, endOfDay);
        int onlinePickup = (int) orderRepository.countByBranchIdAndOrderTypeAndCreatedAtBetween(finalBranchId, OrderType.ONLINE_PICKUP, startOfDay, endOfDay);
        int online = onlineDelivery + onlinePickup;

        ManagerDashboardSummaryDTO.ManagerOrderDistributionDTO distribution = ManagerDashboardSummaryDTO.ManagerOrderDistributionDTO.builder()
                .total(dineIn + online)
                .dineIn(dineIn)
                .online(online)
                .build();

        // 7. Recent Orders
        List<Order> topOrders = orderRepository.findTop5ByBranchIdOrderByCreatedAtDesc(finalBranchId);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        List<ManagerDashboardSummaryDTO.ManagerRecentOrderDTO> recentOrders = topOrders.stream().map(o -> 
                ManagerDashboardSummaryDTO.ManagerRecentOrderDTO.builder()
                        .id(o.getOrderNumber() != null ? o.getOrderNumber() : String.valueOf(o.getId()))
                        .type(o.getOrderType() != null ? o.getOrderType().name().toLowerCase() : "unknown")
                        .status(o.getStatus().name().toLowerCase())
                        .timer(o.getCreatedAt().format(timeFormatter))
                        .build()
        ).collect(Collectors.toList());

        // 8. Staff Availability
        int kitchenTotal = (int) staffRepository.countByBranchIdAndUserRoleName(finalBranchId, "CHEF");
        int kitchenActive = (int) staffRepository.countByBranchIdAndUserRoleNameAndEmploymentStatus(finalBranchId, "CHEF", EmploymentStatus.ACTIVE);
        
        int fleetTotal = (int) staffRepository.countByBranchIdAndUserRoleName(finalBranchId, "RIDER");
        int fleetActive = (int) staffRepository.countByBranchIdAndUserRoleNameAndEmploymentStatus(finalBranchId, "RIDER", EmploymentStatus.ACTIVE);

        ManagerDashboardSummaryDTO.ManagerStaffAvailabilityDTO staff = ManagerDashboardSummaryDTO.ManagerStaffAvailabilityDTO.builder()
                .kitchen(new ManagerDashboardSummaryDTO.ManagerStaffAvailabilityDTO.StaffStats(kitchenActive, kitchenTotal))
                .fleet(new ManagerDashboardSummaryDTO.ManagerStaffAvailabilityDTO.StaffStats(fleetActive, fleetTotal))
                .build();

        return ManagerDashboardSummaryDTO.builder()
                .revenue(revenue)
                .activeOrders(activeOrders)
                .pendingDeliveries(pendingDeliveries)
                .lowStockAlerts(lowStockAlerts)
                .salesTarget(salesTarget)
                .orderDistribution(distribution)
                .recentOrders(recentOrders)
                .staff(staff)
                .fleetActiveDeliveries((int) outDeliveries.stream().filter(o -> o.getBranch().getId().equals(finalBranchId)).count())
                .build();
    }

    private Long resolveBranchId(Long targetBranchId, Long userId) {
        if (targetBranchId != null) {
            return targetBranchId;
        }
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not assigned to any branch as staff"));
        if (staff.getBranch() == null) {
            throw new IllegalArgumentException("Staff member is not assigned to a branch");
        }
        return staff.getBranch().getId();
    }
}
