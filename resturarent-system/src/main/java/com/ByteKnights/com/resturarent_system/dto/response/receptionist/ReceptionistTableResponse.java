package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import com.ByteKnights.com.resturarent_system.entity.TableStatus;
import lombok.*;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReceptionistTableResponse {
    private Long id;
    private Integer tableNumber;
    private Integer capacity;
    private TableStatus status;
    private Integer currentGuestCount;
    private Integer activeOrderCount;
    private LocalDateTime statusUpdatedAt;
}
