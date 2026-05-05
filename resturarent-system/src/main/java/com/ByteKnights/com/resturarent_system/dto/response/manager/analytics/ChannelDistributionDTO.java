package com.ByteKnights.com.resturarent_system.dto.response.manager.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDistributionDTO {
    private String channel; // e.g., "DINE_IN", "DELIVERY", "PICKUP"
    private Long count;
    private BigDecimal revenue;
}
