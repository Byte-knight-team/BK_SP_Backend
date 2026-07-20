package com.ByteKnights.com.resturarent_system.dto.request.procurement;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateVendorRequest {

    @NotBlank(message = "Vendor name is required")
    private String name;

    private String contactPerson;

    private String phone;

    private String email;

    private String address;

    /** Category of goods (e.g., "Produce", "Dairy", "Dry Goods", "Beverages") */
    private String category;
}
