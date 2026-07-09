package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// One data point in the 7-day revenue bar chart
// Same pattern as PeakHourDTO used by the kitchen dashboard
// Returned as a list by GET /api/v1/receptionist/dashboard/revenue
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceptionistRevenuePointDTO {

    // Short day label shown on the X-axis (e.g. "Mon", "Tue")
    private String day;

    // Total revenue collected (paymentStatus = PAID) on that day
    private double revenue;
}
