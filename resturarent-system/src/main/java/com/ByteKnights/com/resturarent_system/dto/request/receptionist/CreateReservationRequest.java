// 2. CreateReservationRequest.java
package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreateReservationRequest {
    @NotNull(message = "Table ID is required")
    private Long tableId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Phone number is required")
    private String customerPhone;

    @NotNull(message = "Reservation time is required")
    @Future(message = "Reservation must be in the future")
    private LocalDateTime reservationTime;

    @Min(value = 1, message = "Guest count must be at least 1")
    private Integer guestCount;
}
