package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ReservationChargeBreakdown {
    private BigDecimal timeCharge;
    private BigDecimal handlingFee;
    private BigDecimal totalCharge;
}
