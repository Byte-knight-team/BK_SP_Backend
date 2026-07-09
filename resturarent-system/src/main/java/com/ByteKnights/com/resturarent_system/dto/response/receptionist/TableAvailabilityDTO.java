package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableAvailabilityDTO {

    private Long tableId;
    private Integer tableNumber;
    private Integer capacity;

    // FREE | RESERVED | OCCUPIED
    private String status;

    // populated only when status == RESERVED (the clashing reservation's window)
    private LocalDateTime conflictStart;
    private LocalDateTime conflictEnd;

    // populated only when status == OCCUPIED (so the receptionist can judge manually)
    private LocalDateTime occupiedSince;
    private Integer activeOrderCount;
}
