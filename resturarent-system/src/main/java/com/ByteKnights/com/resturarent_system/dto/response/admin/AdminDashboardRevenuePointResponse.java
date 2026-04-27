package com.ByteKnights.com.resturarent_system.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardRevenuePointResponse {

    private String date;
    private String dayLabel;
    private BigDecimal revenue;
}
