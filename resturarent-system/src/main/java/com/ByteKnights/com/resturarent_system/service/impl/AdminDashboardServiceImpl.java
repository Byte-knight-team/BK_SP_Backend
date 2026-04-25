package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.admin.AdminDashboardSummaryResponse;
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
import java.util.EnumSet;
import java.util.Set;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = EnumSet.of(
            OrderStatus.OPEN,
            OrderStatus.PLACED,
            OrderStatus.APPROVED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.SERVED,
            OrderStatus.PAID
    );

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

    private Long resolveCurrentAdminBranchIdOrNull() {
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
                .map(staff -> staff.getBranch().getId())
                .orElseThrow(() -> new InvalidOperationException("Admin staff profile not found"));
    }
}
