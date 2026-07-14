package com.ByteKnights.com.resturarent_system.dto.request.kitchen;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request body for a CHEF creating a new menu item via the kitchen-owned menu
 * endpoints ({@code /api/v1/kitchen/menu}).
 *
 * There is no branchId/status/isAvailable field here (unlike the admin-side
 * CreateMenuItemRequest) — the branch is always resolved from the chef's own
 * staff profile, and every item created this way always starts as PENDING and
 * unavailable until an admin approves it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateKitchenMenuItemRequest {

    // Which menu category this item belongs to. Must be an ACTIVE category —
    // the kitchen menu service rejects the request otherwise.
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // Free-text sub-category (e.g. "Starters"). Not a separate table — the
    // kitchen menu service normalizes it to Title Case, same as the admin side.
    @NotBlank(message = "Sub category is required")
    private String subCategory;

    @NotBlank(message = "Name is required")
    @Size(min = 3, message = "Name must be at least 3 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    @DecimalMax(value = "99999999.99", message = "Price must be less than or equal to 99999999.99")
    private BigDecimal price;

    // Cloudinary-hosted image URL, set by CloudinaryImageUpload on the frontend.
    // Mandatory — every menu item must have a photo before it can be submitted.
    @NotBlank(message = "Image is required")
    @Pattern(regexp = "^https?://\\S+$", message = "Image URL must be a valid HTTP/HTTPS URL")
    private String imageUrl;

    @NotNull(message = "Preparation time is required")
    @Positive(message = "Preparation time must be greater than zero")
    @Max(value = 240, message = "Preparation time must be less than or equal to 240 minutes")
    private Integer preparationTime;
}
