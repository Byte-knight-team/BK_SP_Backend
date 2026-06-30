package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailableChefDTO {
    private Long staffId;
    private String chefName;
    private String workStatus;
    private long activeItemCount;
}
