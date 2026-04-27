package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChefCheckInDTO {
    private Long id;        // Staff ID of the Line Chef
    private String fullName; // Chef's Name
}
