package com.ByteKnights.com.resturarent_system.dto.request.customer;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CustomerCreateReservationRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Reservation start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "Reservation end time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Guest count is required")
    @Min(value = 1, message = "Guest count must be at least 1")
    private Integer guestCount;

    private String customerNote;
}
