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
    private int driversOnline;
    private int available;
    private int busy;
    private int pendingDispatch;
    
    private List<DispatchOrderDTO> dispatchOrders;
    private List<DriverStatusDTO> drivers;

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
    }
}
