package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * LEGACY — the old flow where the RECEPTIONIST created a reservation directly and
 * typed the customer's details. In the new customer-initiated model this is being
 * retired (customers request online). Kept only until the new flow fully replaces it.
 */
@Data
public class CreateReservationRequest {

    // One booking can span multiple tables under one customer.
    @NotEmpty(message = "At least one table is required")
    private List<Long> tableIds;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Phone number is required")
    private String customerPhone;

    @NotNull(message = "Reservation time is required")
    private LocalDateTime reservationTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Guest count is required")
    @Min(value = 1, message = "Guest count must be at least 1")
    private Integer guestCount;

    private String notes;
}
