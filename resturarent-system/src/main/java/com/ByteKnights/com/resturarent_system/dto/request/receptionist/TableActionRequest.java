package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TableActionRequest {
    @NotNull(message = "Guest count is required")
    @Min(value = 1, message = "At least 1 guest must be seated")
    private Integer guestCount;
}
