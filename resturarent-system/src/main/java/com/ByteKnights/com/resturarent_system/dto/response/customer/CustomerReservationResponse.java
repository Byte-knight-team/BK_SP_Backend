package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CustomerReservationResponse {
    private Long id;
    private String branchName;
    private Long branchId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer guestCount;
    private String customerNote;
    private String receptionistNote;
    private String status;
    private BigDecimal totalCharge;
    private BigDecimal handlingFee;
    private LocalDateTime paymentDeadline;
    private List<Integer> tableNumbers;
    private LocalDateTime createdAt;
}
