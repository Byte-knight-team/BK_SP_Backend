package com.ByteKnights.com.resturarent_system.dto.response.superadmin;

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
public class SuperAdminBranchRevenueResponse {

    private Long branchId;
    private String branchName;
    private String branchStatus;

    private BigDecimal periodRevenue;
    private Long periodOrderCount;

    private BigDecimal todayRevenue;
    private Long todayOrderCount;

    private BigDecimal averageOrderValue;
}