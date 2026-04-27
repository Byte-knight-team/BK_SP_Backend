package com.ByteKnights.com.resturarent_system.dto.request.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMenuItemRequest {

    private Long branchId;

    private Long categoryId;

    private String subCategory;

    @Size(min = 3, message = "Name must be at least 3 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    @DecimalMax(value = "99999999.99", message = "Price must be less than or equal to 99999999.99")
    private BigDecimal price;

    @Pattern(regexp = "^$|^https?://\\S+$", message = "Image URL must be a valid HTTP/HTTPS URL")
    private String imageUrl;

    private Boolean isAvailable;

    private String status;

    @Positive(message = "Preparation time must be greater than zero")
    @Max(value = 240, message = "Preparation time must be less than or equal to 240 minutes")
    private Integer preparationTime;
}
