package com.ByteKnights.com.resturarent_system.service.impl.kitchen;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PeakHourDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.PopularMealDTO;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.repository.OrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.service.kitchen.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    //Peak hours
    @Override
    public List<PeakHourDTO> getPeakHours() {
        // 1. කලින්ම අපිට ලැබෙන්න ඕන සියලුම time slots ටික 0 count එකත් එක්ක ලෑස්ති කරගමු
        Map<String, Integer> peakHourMap = new LinkedHashMap<>();
        peakHourMap.put("8AM-10AM", 0);
        peakHourMap.put("10AM-12PM", 0);
        peakHourMap.put("12PM-2PM", 0);
        peakHourMap.put("2PM-4PM", 0);
        peakHourMap.put("4PM-6PM", 0);
        peakHourMap.put("6PM-8PM", 0);
        peakHourMap.put("8PM-10PM", 0);

        // 2. Database එකෙන් raw data ටික ගමු (Hour, Count)
        List<Object[]> rawData = orderRepository.findOrderCountByHour();

        // 3. Database එකෙන් ආපු පැය අනුව අදාළ bucket එකට count එක එකතු කරමු
        for (Object[] row : rawData) {
            int hour = ((Number) row[0]).intValue();
            int count = ((Number) row[1]).intValue();

            String slot = null;
            if (hour >= 8 && hour < 10) slot = "8AM-10AM";
            else if (hour >= 10 && hour < 12) slot = "10AM-12PM";
            else if (hour >= 12 && hour < 14) slot = "12PM-2PM";
            else if (hour >= 14 && hour < 16) slot = "2PM-4PM";
            else if (hour >= 16 && hour < 18) slot = "4PM-6PM";
            else if (hour >= 18 && hour < 20) slot = "6PM-8PM";
            else if (hour >= 20 && hour < 22) slot = "8PM-10PM";

            if (slot != null) {
                peakHourMap.put(slot, peakHourMap.get(slot) + count);
            }
        }

        // 4. Map එක DTO list එකකට හරවමු
        List<PeakHourDTO> dtos = new ArrayList<>();
        peakHourMap.forEach((time, mealsCount) -> dtos.add(new PeakHourDTO(time, mealsCount)));

        return dtos;
    }




}
