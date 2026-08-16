package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LineChefCookingStatsDTO {
    private long cookedToday;
    private long cookedTotal;
}
