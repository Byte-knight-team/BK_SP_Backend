package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderCountByTypeDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistRevenueByTypeDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.PaymentRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.ReceptionistDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReceptionistDashboardServiceImpl implements ReceptionistDashboardService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    private Long getBranchId(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        return staff.getBranch().getId();
    }

    @Override
    public ReceptionistDashboardStatsDTO getDashboardStats(String userEmail) {
        Long branchId = getBranchId(userEmail);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

        // New orders
        long newQR = orderRepository.countByBranchIdAndStatusAndOrderTypeAndCreatedAtBetween(
                branchId, OrderStatus.PLACED, OrderType.QR, start, end);
        long newPickup = orderRepository.countByBranchIdAndStatusAndOrderTypeAndCreatedAtBetween(
                branchId, OrderStatus.PLACED, OrderType.ONLINE_PICKUP, start, end);

        // Kitchen orders — QR excludes orders that already have a READY/SERVED item
        long kitchenQR = orderRepository.countKitchenQROrdersWithoutReadyItems(branchId, OrderStatus.PENDING, start, end)
                + orderRepository.countKitchenQROrdersWithoutReadyItems(branchId, OrderStatus.PREPARING, start, end);
        long kitchenPickup = orderRepository.countByBranchIdAndStatusAndOrderTypeAndCreatedAtBetween(
                branchId, OrderStatus.PENDING, OrderType.ONLINE_PICKUP, start, end)
                + orderRepository.countByBranchIdAndStatusAndOrderTypeAndCreatedAtBetween(
                branchId, OrderStatus.PREPARING, OrderType.ONLINE_PICKUP, start, end);

        // Ready orders
        // QR ready = COMPLETED QR + PENDING/PREPARING QR with any READY/SERVED item
        long readyQR = orderRepository.countByBranchIdAndStatusAndOrderTypeAndCreatedAtBetween(
                branchId, OrderStatus.COMPLETED, OrderType.QR, start, end)
                + orderRepository.countQROrdersWithAnyReadyItem(branchId, start, end);
        long readyPickup = orderRepository.countByBranchIdAndStatusAndOrderTypeAndCreatedAtBetween(
                branchId, OrderStatus.COMPLETED, OrderType.ONLINE_PICKUP, start, end);

        // Served
        long servedQR = orderRepository.countByBranchIdAndStatusAndOrderTypeAndCreatedAtBetween(
                branchId, OrderStatus.SERVED, OrderType.QR, start, end);
        long servedPickup = orderRepository.countByBranchIdAndStatusAndOrderTypeAndCreatedAtBetween(
                branchId, OrderStatus.SERVED, OrderType.ONLINE_PICKUP, start, end);

        // Pending payment
        long pendingPaymentQR = orderRepository.countByBranchIdAndPaymentStatusAndOrderTypeAndCreatedAtBetween(
                branchId, PaymentStatus.PENDING, OrderType.QR, start, end);
        long pendingPaymentPickup = orderRepository.countByBranchIdAndPaymentStatusAndOrderTypeAndCreatedAtBetween(
                branchId, PaymentStatus.PENDING, OrderType.ONLINE_PICKUP, start, end);

        // Cash collected today
        double collectedTodayQR = paymentRepository.sumCashCollectedByOrderType(
                branchId, OrderType.QR, start, end).doubleValue();
        double collectedTodayPickup = paymentRepository.sumCashCollectedByOrderType(
                branchId, OrderType.ONLINE_PICKUP, start, end).doubleValue();

        return new ReceptionistDashboardStatsDTO(
                newQR, newPickup,
                kitchenQR, kitchenPickup,
                readyQR, readyPickup,
                servedQR, servedPickup,
                pendingPaymentQR, pendingPaymentPickup,
                collectedTodayQR, collectedTodayPickup
        );
    }

    @Override
    public List<ReceptionistRevenueByTypeDTO> getRevenueByType(String userEmail) {
        Long branchId = getBranchId(userEmail);
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        // Build 7 day slots
        List<ReceptionistRevenueByTypeDTO> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String label = today.minusDays(i).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            result.add(new ReceptionistRevenueByTypeDTO(label, 0.0, 0.0));
        }

        // QR revenue per day
        List<Object[]> qrRows = paymentRepository.findDailyRevenueByOrderType(branchId, "QR", start, end);
        for (Object[] row : qrRows) {
            LocalDate rowDate = ((java.sql.Date) row[0]).toLocalDate();
            double revenue = ((Number) row[1]).doubleValue();
            int index = 6 - (int) (today.toEpochDay() - rowDate.toEpochDay());
            if (index >= 0 && index < result.size()) {
                result.get(index).setQrRevenue(revenue);
            }
        }

        // Pickup revenue per day
        List<Object[]> pickupRows = paymentRepository.findDailyRevenueByOrderType(branchId, "ONLINE_PICKUP", start, end);
        for (Object[] row : pickupRows) {
            LocalDate rowDate = ((java.sql.Date) row[0]).toLocalDate();
            double revenue = ((Number) row[1]).doubleValue();
            int index = 6 - (int) (today.toEpochDay() - rowDate.toEpochDay());
            if (index >= 0 && index < result.size()) {
                result.get(index).setPickupRevenue(revenue);
            }
        }

        return result;
    }

    @Override
    public ReceptionistOrderCountByTypeDTO getOrderCountsByType(String userEmail) {
        Long branchId = getBranchId(userEmail);
        LocalDateTime start = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

        List<Object[]> rows = orderRepository.countCompletedOrdersByType(branchId, start, end);

        long qrCount = 0, pickupCount = 0, deliveryCount = 0;
        for (Object[] row : rows) {
            String type = (String) row[0];
            long count = ((Number) row[1]).longValue();
            if ("QR".equals(type)) qrCount = count;
            else if ("ONLINE_PICKUP".equals(type)) pickupCount = count;
            else if ("ONLINE_DELIVERY".equals(type)) deliveryCount = count;
        }

        return new ReceptionistOrderCountByTypeDTO(qrCount, pickupCount, deliveryCount);
    }
}
