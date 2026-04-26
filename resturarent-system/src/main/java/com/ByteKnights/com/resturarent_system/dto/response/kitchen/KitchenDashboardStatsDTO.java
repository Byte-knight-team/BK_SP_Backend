package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class KitchenDashboardStatsDTO {
    private long pendingOrders;
    private long preparingOrders;
    private long completedOrders;
    private double averagePrepTimeInMinutes;
}
