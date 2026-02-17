package com.ByteKnights.com.resturarent_system.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatsDTO {
    private long openCount;
    private long paidCount;
    private long closedCount;
    private long cancelledCount;
}
