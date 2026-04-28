package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChefDetailsDTO {
    private Long staffId;
    private String fullName;
    private String attendanceStatus; // "PRESENT" or "ABSENT"
    private String workStatus;       // e.g., "AVAILABLE", "COOKING", "OFF_DUTY"
    private Long totalMealsPrepared; // All-time count
}
