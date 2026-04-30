package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.analytics.AnalyticsSummaryDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.analytics.RevenueTrendDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.analytics.ChannelDistributionDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.analytics.PeakHourDTO;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.OrderType;
import com.ByteKnights.com.resturarent_system.entity.InventoryItem;
import com.ByteKnights.com.resturarent_system.entity.PaymentStatus;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
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

        // 5. Channel Distribution (Step 1.4) - Using EXISTING repository methods
        List<ChannelDistributionDTO> channelDistribution = new ArrayList<>();
        for (OrderType type : OrderType.values()) {
            long typeCount = orderRepository.countByBranchIdAndOrderTypeAndCreatedAtBetween(
                    branchId, type, start, end);
            
            BigDecimal typeRevenue = orderRepository.sumFinalAmountByBranchIdAndOrderTypeAndStatusIn(
                    branchId, type, Collections.singletonList(OrderStatus.COMPLETED));
            
            if (typeCount > 0 || (typeRevenue != null && typeRevenue.compareTo(BigDecimal.ZERO) > 0)) {
                channelDistribution.add(ChannelDistributionDTO.builder()
                        .channel(type.name())
                        .count(typeCount)
                        .revenue(typeRevenue != null ? typeRevenue : BigDecimal.ZERO)
                        .build());
            }
        }

        // 6. Total Inventory Value (Step 1.5)
        List<InventoryItem> items = inventoryItemRepository.findByBranchId(branchId);
        BigDecimal totalInventoryValue = items.stream()
                .map(item -> {
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                    BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    return qty.multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7. Peak Hours (Step 4.1)
        List<com.ByteKnights.com.resturarent_system.entity.Order> orders = orderRepository.findByBranchIdAndPaymentStatusInAndCreatedAtBetween(
                branchId, Arrays.asList(PaymentStatus.values()), start, end);
        
        Map<Integer, Long> hourCounts = new TreeMap<>(); // TreeMap for sorted hours
        // Initialize all 24 hours
        for (int i = 0; i < 24; i++) hourCounts.put(i, 0L);
        
        orders.forEach(order -> {
            int hour = order.getCreatedAt().getHour();
            hourCounts.put(hour, hourCounts.get(hour) + 1);
        });

        List<PeakHourDTO> peakHours = hourCounts.entrySet().stream()
                .map(entry -> PeakHourDTO.builder()
                        .hour(String.format("%02d:00", entry.getKey()))
                        .orderCount(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        return AnalyticsSummaryDTO.builder()
                .netRevenue(netRevenue != null ? netRevenue : BigDecimal.ZERO)
                .orderCount(orderCount)
                .avgPrepTimeMinutes(avgPrepTime != null ? avgPrepTime : 0.0)
                .revenueTrends(revenueTrends)
                .channelDistribution(channelDistribution)
                .totalInventoryValue(totalInventoryValue)
                .peakHours(peakHours)
                .build();
    }
}