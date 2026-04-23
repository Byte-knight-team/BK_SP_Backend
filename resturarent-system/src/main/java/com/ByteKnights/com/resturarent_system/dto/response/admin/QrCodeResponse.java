package com.ByteKnights.com.resturarent_system.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCodeResponse {

    private Long id;
    private Long branchId;
    private Long tableId;
    private Boolean isActive;
    private LocalDateTime lastGeneratedAt;
    private LocalDateTime revokedAt;
    private String revokedReason;
    private Long createdByStaffId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
