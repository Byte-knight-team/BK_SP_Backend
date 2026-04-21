package com.ByteKnights.com.resturarent_system.service.impl.kitchen;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PopularMealDTO;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.repository.OrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.service.kitchen.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KitchenServiceImpl implements KitchenService {

    final OrderRepository orderRepository;
    final OrderItemRepository orderItemRepository;

    //kitchen dashboard stat
    @Override
    public KitchenDashboardStatsDTO getKitchenDashboardStats() {

        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long preparing = orderRepository.countByStatus(OrderStatus.PREPARING);
        long completed = orderRepository.countByStatus(OrderStatus.COMPLETED);

        Double avgTime = orderRepository.getAveragePreparationTime();
        return new KitchenDashboardStatsDTO(
                pending,
                preparing,
                completed,
                avgTime != null ? Math.round(avgTime * 100.0) / 100.0 : 0.0 // Round for 2 decimal digits
        );
    }

    //most popular meals
    @Override
    public List<PopularMealDTO> getMostPopularMeals() {
        List<Object[]> topMeals = orderItemRepository.findTop5PopularMeals();
        Long totalSold = orderItemRepository.getTotalItemsSoldInLast24Hours();

        //Avoid Division by Zero
        //Return Empty List if no meals sold in last 24 hours
        if (totalSold == null || totalSold == 0) {
            return new ArrayList<>();
        }

        List<PopularMealDTO> popularMealDtoList = new ArrayList<>();

        for (Object[] row : topMeals) {
            // Data Extraction
            // row[0] = item_name (String)
            // row[1] = mealCount (Number)
            String mealName = (String) row[0];

            // SQL SUM() typically returns Long/BigInteger. Direct cast Long to Integer will fail.
            // Using 'Number' (the parent class) prevents crashes by safely handling any numeric type.
            Number countNumber = (Number) row[1];
            int count = countNumber.intValue();

            double percentage = (count * 100.0) / totalSold;

            // Mapping to DTO
            popularMealDtoList.add(new PopularMealDTO(
                    mealName,
                    count,
                    Math.round(percentage * 100.0) / 100.0
            ));
        }

        return popularMealDtoList;
    }


}
