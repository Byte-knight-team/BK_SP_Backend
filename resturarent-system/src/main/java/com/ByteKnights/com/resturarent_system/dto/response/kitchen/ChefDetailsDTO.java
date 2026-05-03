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
    private String clockInTime;    // e.g., "08:30 AM" or "Not Checked In"
    private String clockOutTime;   // e.g., "05:30 PM" or "Not Checked Out"
    private String workStatus;     // e.g., "AVAILABLE", "COOKING", "OFF_DUTY"
    private long totalMealsToday;  // Only meals finished TODAY
}
