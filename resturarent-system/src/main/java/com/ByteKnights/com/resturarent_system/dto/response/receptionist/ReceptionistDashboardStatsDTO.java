package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Same pattern as KitchenDashboardStatsDTO — one flat DTO holding all KPI card values
// Returned by GET /api/v1/receptionist/dashboard/stats
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceptionistDashboardStatsDTO {

    // Orders waiting to be sent to kitchen (status = PLACED)
    private long openOrders;

    // Orders already sent and being prepared (status = PENDING)
    private long inKitchenOrders;

    // Orders kitchen finished — ready to serve/hand over (status = COMPLETED)
    private long readyToServe;

    // Orders where customer hasn't paid cash yet (paymentStatus = PENDING)
    private long cashDue;

    // Breakdown per status for the pipeline bar chart on the dashboard
    private long placedCount;
    private long pendingCount;
    private long completedCount;
    private long onHoldCount;
    private long servedCount;
}
