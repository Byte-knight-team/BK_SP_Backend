package com.ByteKnights.com.resturarent_system.service.impl.kitchen;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.repository.kitchen.KitchenOrderRepository;
import com.ByteKnights.com.resturarent_system.service.kitchen.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KitchenServiceImpl implements KitchenService {

    final KitchenOrderRepository kitchenOrderRepository;

    @Override
    public KitchenDashboardStatsDTO getKitchenDashboardStats() {

        long pending = kitchenOrderRepository.countByStatus(OrderStatus.PENDING);
        long preparing = kitchenOrderRepository.countByStatus(OrderStatus.PREPARING);
        long completed = kitchenOrderRepository.countByStatus(OrderStatus.COMPLETED);

        Double avgTime = kitchenOrderRepository.getAveragePreparationTime();
        return new KitchenDashboardStatsDTO(
                pending,
                preparing,
                completed,
                avgTime != null ? Math.round(avgTime * 100.0) / 100.0 : 0.0 // Round for 2 decimal digits
        );
    }
}
