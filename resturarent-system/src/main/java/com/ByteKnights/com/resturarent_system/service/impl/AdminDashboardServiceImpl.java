package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardOrderFlowResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardRevenuePointResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardSummaryResponse;
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.PaymentStatus;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.AdminDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int MIN_TREND_DAYS = 3;
    private static final int MAX_TREND_DAYS = 31;
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    // Dashboard "active orders" are those still in operational flow (not finished/cancelled).
    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = EnumSet.of(
            //OrderStatus.OPEN,
            OrderStatus.PLACED,
            OrderStatus.APPROVED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.SERVED
            //OrderStatus.PAID
    );

    // Revenue should only include orders that have a successful payment outcome.
    private static final Set<PaymentStatus> REVENUE_PAYMENT_STATUSES = EnumSet.of(
            PaymentStatus.PAID,
            PaymentStatus.SUCCESS
    );

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    public AdminDashboardServiceImpl(OrderRepository orderRepository,
                                     UserRepository userRepository,
                                     StaffRepository staffRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary() {
        // ADMIN users are branch-scoped, while SUPER_ADMIN gets global aggregates.
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();

        long totalOrders;
        long activeOrderCount;
        long activeUsers;
        BigDecimal totalRevenue;

        if (adminBranchId == null) {
            totalOrders = orderRepository.count();
            activeOrderCount = orderRepository.countByStatusIn(ACTIVE_ORDER_STATUSES);
            activeUsers = userRepository.countActiveUsers();
            totalRevenue = orderRepository.sumFinalAmountByPaymentStatusIn(REVENUE_PAYMENT_STATUSES);
        } else {
            totalOrders = orderRepository.countByBranchId(adminBranchId);
            activeOrderCount = orderRepository.countByBranchIdAndStatusIn(adminBranchId, ACTIVE_ORDER_STATUSES);
            activeUsers = userRepository.countActiveUsersByBranchId(adminBranchId);
            totalRevenue = orderRepository.sumFinalAmountByBranchIdAndPaymentStatusIn(
                    adminBranchId,
                    REVENUE_PAYMENT_STATUSES
            );
        }

        return AdminDashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .activeUsers(activeUsers)
                .activeOrderCount(activeOrderCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardOrderFlowResponse getOrderFlowSummary() {
        // Same branch/global scope rule used in summary and trend endpoints.
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();

        // Operational flow buckets for the dashboard's "current order pipeline" cards.
        long preparingCount = countOrdersForScope(adminBranchId, OrderStatus.PREPARING);
        long readyCount = countOrdersForScope(adminBranchId, OrderStatus.READY);
        long inDeliveryCount = countOrdersForScope(adminBranchId, OrderStatus.OUT_FOR_DELIVERY);
        long completedCount = countOrdersForScope(adminBranchId, OrderStatus.COMPLETED);

        return AdminDashboardOrderFlowResponse.builder()
                .preparingCount(preparingCount)
                .readyCount(readyCount)
                .inDeliveryCount(inDeliveryCount)
                .completedCount(completedCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDashboardRevenuePointResponse> getRevenueTrend(int days) {
        // Clamp user input to a safe reporting window.
        int normalizedDays = normalizeTrendDays(days);
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(normalizedDays - 1L);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = LocalDateTime.now();

        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        LocalDate cursor = startDate;
        // Pre-fill each day so the chart has continuous points, even for zero-revenue days.
        while (!cursor.isAfter(endDate)) {
            revenueByDate.put(cursor, BigDecimal.ZERO);
            cursor = cursor.plusDays(1);
        }

        List<Order> paidOrders = fetchRevenueOrdersForScope(adminBranchId, startDateTime, endDateTime);
        for (Order order : paidOrders) {
            // Defensive guard for legacy/incomplete rows.
            if (order.getCreatedAt() == null) {
                continue;
            }

            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            // Ignore rows outside the prebuilt date window.
            if (!revenueByDate.containsKey(orderDate)) {
                continue;
            }

            BigDecimal amount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
            revenueByDate.put(orderDate, revenueByDate.get(orderDate).add(amount));
        }

        List<AdminDashboardRevenuePointResponse> points = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : revenueByDate.entrySet()) {
            points.add(AdminDashboardRevenuePointResponse.builder()
                    .date(entry.getKey().toString())
                    .dayLabel(entry.getKey().format(DAY_LABEL_FORMATTER))
                    .revenue(entry.getValue())
                    .build());
        }

        return points;
    }

    private List<Order> fetchRevenueOrdersForScope(Long adminBranchId, LocalDateTime start, LocalDateTime end) {
        // Reuse scoped query variants to keep ADMIN and SUPER_ADMIN behavior consistent.
        if (adminBranchId == null) {
            return orderRepository.findByPaymentStatusInAndCreatedAtBetween(REVENUE_PAYMENT_STATUSES, start, end);
        }

        return orderRepository.findByBranchIdAndPaymentStatusInAndCreatedAtBetween(
                adminBranchId,
                REVENUE_PAYMENT_STATUSES,
                start,
                end
        );
    }

    private int normalizeTrendDays(int days) {
        // Non-positive values fall back to the standard dashboard range.
        if (days <= 0) {
            return DEFAULT_TREND_DAYS;
        }
        // Enforce minimum chart width for readability.
        if (days < MIN_TREND_DAYS) {
            return MIN_TREND_DAYS;
        }
        // Cap range to avoid expensive unbounded trend queries.
        return Math.min(days, MAX_TREND_DAYS);
    }

    private long countOrdersForScope(Long adminBranchId, OrderStatus status) {
        // Null branch means global scope (SUPER_ADMIN or non-ADMIN context).
        if (adminBranchId == null) {
            return orderRepository.countByStatus(status);
        }
        // ADMIN users are strictly constrained to their assigned branch.
        return orderRepository.countByBranchIdAndStatus(adminBranchId, status);
    }

    private Long resolveCurrentAdminBranchIdOrNull() {
        // Returns null for non-ADMIN users so callers can treat them as global scope.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof JwtUserPrincipal jwtUser)
                || jwtUser.getUser() == null
                || jwtUser.getUser().getId() == null) {
            throw new InvalidOperationException("Authenticated ADMIN user not found");
        }

        return staffRepository.findByUserId(jwtUser.getUser().getId())
            // Branch association is mandatory for ADMIN dashboard scoping.
                .map(staff -> staff.getBranch().getId())
                .orElseThrow(() -> new InvalidOperationException("Admin staff profile not found"));
    }
}
