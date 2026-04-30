package com.ByteKnights.com.resturarent_system.dto.response.manager.analytics;

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
public class AnalyticsSummaryDTO {
    private BigDecimal netRevenue;
    private Long orderCount;
    private Double avgPrepTimeMinutes;
    private BigDecimal totalInventoryValue;
    
    private List<RevenueTrendDTO> revenueTrends;
    private List<ChannelDistributionDTO> channelDistribution;
    private List<PeakHourDTO> peakHours;
}
