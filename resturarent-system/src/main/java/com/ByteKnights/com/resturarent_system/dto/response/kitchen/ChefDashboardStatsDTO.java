package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChefDashboardStatsDTO {
    private long totalChefs;
    private long onDutyChefs;
    private long offDutyChefs;
    private long availableChefs;
}
