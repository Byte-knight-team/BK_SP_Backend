package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.analytics.AnalyticsSummaryDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.analytics.RevenueTrendDTO;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.ManagerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerAnalyticsServiceImpl implements ManagerAnalyticsService {

    private final OrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryDTO getBranchAnalyticsSummary(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // 1. Calculate Net Revenue (Step 1.3)
        BigDecimal netRevenue = orderRepository.sumFinalAmountByBranchIdAndStatusAndCreatedAtBetween(
                branchId, OrderStatus.COMPLETED, start, end);

        // 2. Calculate Order Count (Step 1.3)
        long orderCount = orderRepository.countByBranchIdAndStatusAndCreatedAtBetween(
                branchId, OrderStatus.COMPLETED, start, end);

        // 3. Average Prep Time (Step 1.3)
        Double avgPrepTime = orderRepository.getAveragePreparationTime();

        // 4. Revenue Trends (Step 1.3)
        List<Object[]> trendData = orderRepository.findRevenueTrendByBranchAndDates(branchId, start, end);
        List<RevenueTrendDTO> revenueTrends = trendData.stream().map(row -> {
            String label = row[0] != null ? row[0].toString() : "Unknown";
            BigDecimal revenue = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            Long count = row[2] != null ? Long.parseLong(row[2].toString()) : 0L;
            
            return RevenueTrendDTO.builder()
                    .label(label)
                    .revenue(revenue)
                    .orderCount(count)
                    .build();
        }).collect(Collectors.toList());

        return AnalyticsSummaryDTO.builder()
                .netRevenue(netRevenue != null ? netRevenue : BigDecimal.ZERO)
                .orderCount(orderCount)
                .avgPrepTimeMinutes(avgPrepTime != null ? avgPrepTime : 0.0)
                .revenueTrends(revenueTrends)
                .channelDistribution(new ArrayList<>()) // Placeholder for Step 1.4
                .totalInventoryValue(BigDecimal.ZERO)  // Placeholder for Step 1.5
                .build();
    }
}
