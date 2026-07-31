package com.ByteKnights.com.resturarent_system.dto.request.procurement;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateVendorRequest {

    @NotBlank(message = "Vendor name is required")
    private String name;

    private String contactPerson;

    private String phone;

    private String email;

    private String address;

    private String category;

    /** Allows re-activating a previously deactivated vendor */
    private boolean isActive = true;
}
