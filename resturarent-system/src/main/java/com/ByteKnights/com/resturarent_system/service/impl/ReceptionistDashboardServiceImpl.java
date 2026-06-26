package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistRevenuePointDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
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
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    // ── helper: resolve branch from logged-in receptionist ──────────────────
    // Same pattern used in KitchenDashboardServiceImpl and ReceptionistOrderServiceImpl
    private Long getBranchId(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        return staff.getBranch().getId();
    }

    // ── B1: KPI stats + pipeline breakdown ──────────────────────────────────
    @Override
    public ReceptionistDashboardStatsDTO getDashboardStats(String userEmail) {
        Long branchId = getBranchId(userEmail);

        // Count from midnight today — same approach as KitchenDashboardServiceImpl
        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfToday   = LocalDateTime.now().with(LocalTime.MAX);

        // Receptionist only handles QR (dine-in) and ONLINE_PICKUP orders
        List<OrderType> receptionistTypes = List.of(OrderType.QR, OrderType.ONLINE_PICKUP);

        // KPI card counts — all scoped to today + receptionist order types
        long openOrders    = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.PLACED,     startOfToday);
        long inKitchen     = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.PENDING,    startOfToday);
        long readyToServe  = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.COMPLETED,  startOfToday);

        // Cash due = orders where payment hasn't been collected yet
        long cashDue = orderRepository.countByBranchIdAndPaymentStatusAndOrderTypeInAndCreatedAtBetween(
                branchId, PaymentStatus.PENDING, receptionistTypes, startOfToday, endOfToday);

        // Pipeline breakdown for the bar chart (all statuses, today only)
        long placed    = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.PLACED,    startOfToday);
        long pending   = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.PENDING,   startOfToday);
        long completed = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.COMPLETED, startOfToday);
        long onHold    = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.ON_HOLD,   startOfToday);
        long served    = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.SERVED,    startOfToday);

        return new ReceptionistDashboardStatsDTO(
                openOrders, inKitchen, readyToServe, cashDue,
                placed, pending, completed, onHold, served
        );
    }

    // ── B2: Last 7 days revenue ──────────────────────────────────────────────
    @Override
    public List<ReceptionistRevenuePointDTO> getLast7DaysRevenue(String userEmail) {
        Long branchId = getBranchId(userEmail);

        // Build a slot for each of the last 7 days (oldest → newest), defaulting to 0
        // Same concept as KitchenDashboardServiceImpl building PeakHourDTO slots first
        List<ReceptionistRevenuePointDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            // Short day name label for the chart X-axis (e.g. "Mon", "Tue")
            String dayLabel = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            result.add(new ReceptionistRevenuePointDTO(dayLabel, 0.0));
        }

        // Fetch actual revenue data — reuses the existing query in OrderRepository
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        LocalDateTime end   = today.atTime(LocalTime.MAX);
        List<Object[]> rawData = orderRepository.findRevenueTrendByBranchAndDates(branchId, start, end);

        // Map each raw row back to the correct day slot
        for (Object[] row : rawData) {
            // row[0] = DATE(created_at), row[1] = SUM(final_amount)
            LocalDate rowDate = ((java.sql.Date) row[0]).toLocalDate();
            double revenue    = ((Number) row[1]).doubleValue();

            // Find matching slot by index (oldest day = index 0)
            int index = (int) (today.toEpochDay() - rowDate.toEpochDay());
            int slotIndex = 6 - index;
            if (slotIndex >= 0 && slotIndex < result.size()) {
                result.get(slotIndex).setRevenue(revenue);
            }
        }

        return result;
    }
}
