package com.ByteKnights.com.resturarent_system.dto.response.delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrderDTO {
    private Long id;
    private String orderNumber;
    private String location;
    private String paymentType;
    private BigDecimal amount;
    private String status; // e.g. "ASSIGNED", "ACCEPTED"
}
