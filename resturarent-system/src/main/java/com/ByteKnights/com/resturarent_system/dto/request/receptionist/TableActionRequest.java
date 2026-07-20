package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Body for table-floor actions that need a party size — walk-in "occupy" and
 * "update guest count" on the Table Management page.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TableActionRequest {
    // Number of guests being seated at (or updated for) the table.
    @NotNull(message = "Guest count is required")
    @Min(value = 1, message = "At least 1 guest must be seated")
    private Integer guestCount;
}
