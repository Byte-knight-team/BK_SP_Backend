package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class OrderPlacementResponse {
    private Long orderId;
    private String orderNumber;
    private BigDecimal finalAmount;
}
