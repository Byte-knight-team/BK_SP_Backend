package com.ByteKnights.com.resturarent_system.dto.response.delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrderDTO {
    private Long id;
    private String orderNumber;
    private String location;
    private String deliveryAddress;
    private String customerName;
    private String customerPhone;
    private String paymentType;
    private BigDecimal amount;
    private String status; // e.g. "ASSIGNED", "ACCEPTED"

    // Customer delivery destination coordinates
    private Double latitude;
    private Double longitude;

    // Restaurant / branch pickup coordinates (populated for all orders; used as the
    // pickup pin on the mobile map, especially for RE-DISPATCH orders)
    private Double branchLatitude;
    private Double branchLongitude;
    private String branchName;

    // True when this delivery assignment was created after a prior CANCELLED delivery
    // for the same order — triggers the RE-DISPATCH badge and dual-pin map on mobile
    private boolean isRedispatch;
}
