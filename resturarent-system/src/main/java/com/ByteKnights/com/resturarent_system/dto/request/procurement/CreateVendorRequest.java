package com.ByteKnights.com.resturarent_system.dto.request.procurement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class CreateVendorRequest {

    @NotBlank(message = "Vendor name is required")
    private String name;

    @NotBlank(message = "Contact person is required")
    private String contactPerson;

    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be 10 digits")
    @NotBlank(message = "Phone number is required")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    /** Category of goods (e.g., "Produce", "Dairy", "Dry Goods", "Beverages") */
    @NotBlank(message = "Category is required")
    private String category;
}
