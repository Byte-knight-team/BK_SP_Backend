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
public class RevenueTrendDTO {
    private String label; // e.g., "2024-04-28" or "10:00"
    private BigDecimal revenue;
    private Long orderCount;
}
