package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of an availability check for a time slot: whether a booking is possible and
 * the per-table breakdown (each table tagged FREE / OCCUPIED / BLOCKED).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckAvailabilityResponse {

    private boolean possible;          // true if at least one matching table is bookable (FREE or OCCUPIED)
    private String reason;             // why it's not possible (lead time / no table fits / all reserved)
    private LocalDateTime earliestAllowed; // now + minimum lead time (for the "book earlier" message)
    private List<TableAvailabilityDTO> tables; // eligible tables by size, each tagged
}
