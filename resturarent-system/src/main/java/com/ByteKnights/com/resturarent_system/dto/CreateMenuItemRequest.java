package com.ByteKnights.com.resturarent_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class CreateMenuItemRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private String subCategory;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    private BigDecimal price;

    private String imageUrl;

    private Boolean isAvailable;

    private String status;

    @Positive(message = "Preparation time must be greater than zero")
    private Integer preparationTime;
}
