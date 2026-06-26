package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PeakHourDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PopularMealDTO;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.OrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.KitchenDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KitchenDashboardServiceImpl implements KitchenDashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    @Override
    public KitchenDashboardStatsDTO getKitchenDashboardStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();
        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIN);

        long pending = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.PENDING, startOfToday);
        long preparing = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.PREPARING, startOfToday);
        long completed = orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(branchId, OrderStatus.COMPLETED, startOfToday);

        Double avgTime = orderRepository.getAveragePreparationTimeTodayByBranch(branchId, startOfToday);

        return new KitchenDashboardStatsDTO(
                pending,
                preparing,
                completed,
                avgTime != null ? Math.round(avgTime * 100.0) / 100.0 : 0.0
        );
    }

    @Override
    public List<PopularMealDTO> getMostPopularMealsInLast7Days(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        List<Object[]> topMeals = orderItemRepository.findTop5PopularMealsInLast7DaysByBranch(branchId);
        Long totalSold = orderItemRepository.getTotalItemsSoldInLast7DaysByBranch(branchId);

        if (totalSold == null || totalSold == 0) {
            return new ArrayList<>();
        }

        List<PopularMealDTO> popularMealDtoList = new ArrayList<>();

        for (Object[] row : topMeals) {
            String mealName = (String) row[0];
            Number countNumber = (Number) row[1];
            int count = countNumber.intValue();
            double percentage = (count * 100.0) / totalSold;

            popularMealDtoList.add(new PopularMealDTO(
                    mealName,
                    count,
                    Math.round(percentage * 100.0) / 100.0
            ));
        }

        return popularMealDtoList;
    }

    @Override
    public List<PeakHourDTO> getPeakHoursInLast7Days(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        List<PeakHourDTO> dtos = new ArrayList<>();
        dtos.add(new PeakHourDTO("8AM-10AM", 0));
        dtos.add(new PeakHourDTO("10AM-12PM", 0));
        dtos.add(new PeakHourDTO("12PM-2PM", 0));
        dtos.add(new PeakHourDTO("2PM-4PM", 0));
        dtos.add(new PeakHourDTO("4PM-6PM", 0));
        dtos.add(new PeakHourDTO("6PM-8PM", 0));
        dtos.add(new PeakHourDTO("8PM-10PM", 0));

        List<Object[]> rawData = orderRepository.findOrderCountByHourByBranch(branchId);

        for (Object[] row : rawData) {
            int hour = ((Number) row[0]).intValue();
            int count = ((Number) row[1]).intValue();

            if (hour >= 8 && hour < 10) {
                dtos.get(0).setOrdersCount(dtos.get(0).getOrdersCount() + count);
            } else if (hour >= 10 && hour < 12) {
                dtos.get(1).setOrdersCount(dtos.get(1).getOrdersCount() + count);
            } else if (hour >= 12 && hour < 14) {
                dtos.get(2).setOrdersCount(dtos.get(2).getOrdersCount() + count);
            } else if (hour >= 14 && hour < 16) {
                dtos.get(3).setOrdersCount(dtos.get(3).getOrdersCount() + count);
            } else if (hour >= 16 && hour < 18) {
                dtos.get(4).setOrdersCount(dtos.get(4).getOrdersCount() + count);
            } else if (hour >= 18 && hour < 20) {
                dtos.get(5).setOrdersCount(dtos.get(5).getOrdersCount() + count);
            } else if (hour >= 20 && hour < 22) {
                dtos.get(6).setOrdersCount(dtos.get(6).getOrdersCount() + count);
            }
        }
        return dtos;
    }
}
