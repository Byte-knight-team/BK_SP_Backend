package com.ByteKnights.com.resturarent_system.dto.response.manager.procurement;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorDTO {
    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String category;
    private boolean isActive;
    /** Number of active (SUBMITTED or PARTIALLY_RECEIVED) POs for this vendor */
    private long activePoCount;
}
