package com.ByteKnights.com.resturarent_system.dto.response.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDriverSummaryDTO {
    private int available;
    private int activeDeliveries;
    private int pendingDispatch;

    // Count of open delivery alerts (orders needing re-assignment after driver cancellation)
    private int deliveryAlerts;

    private List<DispatchOrderDTO> dispatchOrders;
    private List<DriverStatusDTO> drivers;
    private List<DeliveryHistoryDTO> deliveryHistory;

    // Orders whose driver cancelled — awaiting manager re-assignment
    private List<DeliveryAlertDTO> deliveryAlertList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchOrderDTO {
        private String id;
        private Long orderId;
        private String status;
        private String customerName;
        private String zone;
        private String distance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverStatusDTO {
        private Long id;
        private String name;
        private String avatar;
        private Double rating;
        private String status;
        private CurrentTaskDTO currentTask;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentTaskDTO {
        private String orderId;
        private String eta;
        private String assignedTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryHistoryDTO {
        private String orderId;
        private String deliveryStatus;
        private String driverName;
        private String resolvedAt;
        private String cancelledReason;
    }

    /**
     * Represents a single open delivery alert: a delivery that was cancelled
     * by a driver and whose parent order has been reverted to COMPLETED,
     * ready for the manager to re-assign to another driver.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAlertDTO {
        private Long deliveryId;
        private Long orderId;
        private String orderNumber;
        private String customerName;
        private String deliveryAddress;

        // Customer drop-off coordinates (for manager's location display)
        private Double customerLatitude;
        private Double customerLongitude;

        // Restaurant pickup coordinates (new driver must go here first)
        private Double branchLatitude;
        private Double branchLongitude;
        private String branchName;

        // Driver who cancelled
        private String cancelledDriverName;
        private String cancelledReason;
        private String cancelledAt;
    }
}
