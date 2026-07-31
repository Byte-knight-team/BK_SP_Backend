package com.ByteKnights.com.resturarent_system.dto.response.manager.procurement;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Summary data for the top-of-page summary cards on the Procurement page.
 */
@Data
@Builder
public class ProcurementSummaryDTO {
    /** Total number of active (non-deactivated) vendors for this branch */
    private long totalActiveVendors;
    /** POs currently in SUBMITTED or PARTIALLY_RECEIVED state */
    private long activePendingPos;
    /** Total spend from confirmed GRNs in the current calendar month */
    private BigDecimal totalMonthlySpend;
    /** Total number of GRNs recorded this month */
    private long totalGrnsThisMonth;
}
