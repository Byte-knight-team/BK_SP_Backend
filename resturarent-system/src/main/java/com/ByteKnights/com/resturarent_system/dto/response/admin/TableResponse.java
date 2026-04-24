package com.ByteKnights.com.resturarent_system.dto.response.admin;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * API response model for table details.
 */
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
