package com.ByteKnights.com.resturarent_system.dto.response.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardSummaryDTO {
    
    private BigDecimal revenue;
    private int activeOrders;
    private int pendingDeliveries;
    private int lowStockAlerts;
    
    private ManagerSalesTargetDTO salesTarget;
    private ManagerOrderDistributionDTO orderDistribution;
    private List<ManagerRecentOrderDTO> recentOrders;
    
    private ManagerStaffAvailabilityDTO staff;
    private int fleetActiveDeliveries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerSalesTargetDTO {
        private BigDecimal current;
        private BigDecimal goal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerOrderDistributionDTO {
        private int total;
        private int dineIn;
        private int online;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerRecentOrderDTO {
        private Long id;
        private String type; // 'online' or 'dine-in'
        private String status; // 'active', 'done', etc.
        private String timer; // e.g., '15:22'
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerStaffAvailabilityDTO {
        private StaffStats kitchen;
        private StaffStats fleet;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class StaffStats {
            private int active;
            private int total;
        }
    }
}
