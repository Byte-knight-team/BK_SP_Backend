package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceptionistDashboardStatsDTO {

    private long newQR;
    private long newPickup;
    private long newDelivery;

    private long kitchenQR;
    private long kitchenPickup;
    private long kitchenDelivery;

    private long readyQR;
    private long readyPickup;
    private long readyDelivery;

    private long servedQR;
    private long servedPickup;

    private long pendingPaymentQR;
    private long pendingPaymentPickup;

    private double collectedTodayQR;
    private double collectedTodayPickup;
}
