package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateReservationRequest {

    @NotNull(message = "Table ID is required")
    private Long tableId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Phone number is required")
    private String customerPhone;

    @NotNull(message = "Reservation time is required")
    private LocalDateTime reservationTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private Integer guestCount;

    private String notes;
}
