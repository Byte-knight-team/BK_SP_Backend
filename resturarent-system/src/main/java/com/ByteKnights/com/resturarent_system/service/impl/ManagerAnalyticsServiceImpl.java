package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.analytics.*;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.ManagerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ManagerAnalyticsServiceImpl implements ManagerAnalyticsService {

    private final OrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StaffRepository staffRepository;
    private final OrderItemRepository orderItemRepository;

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

        // 8. Top Selling Items (Step 4.2)
        List<Long> orderIds = orders.stream().map(com.ByteKnights.com.resturarent_system.entity.Order::getId).collect(Collectors.toList());
        List<TopSellingItemDTO> topSellingItems = new ArrayList<>();
        
        if (!orderIds.isEmpty()) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderIdIn(orderIds);
            Map<String, TopSellingItemDTO> itemSummary = new HashMap<>();
            
            for (OrderItem oi : orderItems) {
                String name = oi.getItemName();
                TopSellingItemDTO dto = itemSummary.getOrDefault(name, TopSellingItemDTO.builder()
                        .itemName(name)
                        .quantity(0L)
                        .revenue(BigDecimal.ZERO)
                        .build());
                
                dto.setQuantity(dto.getQuantity() + (oi.getQuantity() != null ? oi.getQuantity() : 0));
                dto.setRevenue(dto.getRevenue().add(oi.getSubtotal() != null ? oi.getSubtotal() : BigDecimal.ZERO));
                itemSummary.put(name, dto);
            }
            
            topSellingItems = itemSummary.values().stream()
                    .sorted((a, b) -> b.getQuantity().compareTo(a.getQuantity()))
                    .limit(5)
                    .collect(Collectors.toList());
        }

        // 9. Inventory Health (Step 4.3)
        Map<String, InventoryCategoryDTO> categorySummary = new HashMap<>();
        for (InventoryItem item : items) {
            String cat = item.getCategory() != null ? item.getCategory() : "Uncategorized";
            InventoryCategoryDTO dto = categorySummary.getOrDefault(cat, InventoryCategoryDTO.builder()
                    .category(cat)
                    .value(BigDecimal.ZERO)
                    .itemCount(0L)
                    .build());
            
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal val = qty.multiply(price);
            
            dto.setValue(dto.getValue().add(val));
            dto.setItemCount(dto.getItemCount() + 1);
            categorySummary.put(cat, dto);
        }

        List<InventoryCategoryDTO> inventoryByCategory = new ArrayList<>(categorySummary.values());

        return AnalyticsSummaryDTO.builder()
                .netRevenue(netRevenue != null ? netRevenue : BigDecimal.ZERO)
                .orderCount(orderCount)
                .avgPrepTimeMinutes(avgPrepTime != null ? avgPrepTime : 0.0)
                .revenueTrends(revenueTrends)
                .channelDistribution(channelDistribution)
                .totalInventoryValue(totalInventoryValue)
                .peakHours(peakHours)
                .topSellingItems(topSellingItems)
                .inventoryByCategory(inventoryByCategory)
                .build();
    }
}