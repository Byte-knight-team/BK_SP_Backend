package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponseDTO {

    private Long id;
    // A booking can cover multiple tables under one customer.
    private List<Long> tableIds;
    private List<Integer> tableNumbers;
    private String customerName;
    private String customerPhone;
    private LocalDateTime reservationTime;
    private LocalDateTime endTime;
    private Integer guestCount;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
}
