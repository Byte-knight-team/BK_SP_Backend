package com.ByteKnights.com.resturarent_system.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableResponse {

    private Long id;
    private Integer tableNumber;
    private Integer capacity;
    private String status;
    private Long branchId;
    private String branchName;
    private Integer currentGuestCount;
    private Integer activeOrderCount;
    private LocalDateTime createdAt;
}
