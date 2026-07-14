package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A menu item as seen by the CHEF who owns it, returned from the kitchen-owned
 * menu endpoints.
 *
 * Carries BOTH the item's own approval status ({@code status}: PENDING/ACTIVE/
 * REJECTED) and the category's status ({@code categoryStatus}: ACTIVE/INACTIVE)
 * so the frontend can compute whether the item is "effectively active" —
 * it only actually shows to customers when status is ACTIVE AND the category
 * is also ACTIVE. If it's inactive for either reason (or both), the frontend
 * uses these two fields to explain why.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitchenMenuItemResponse {

    private Long id;

    private Long categoryId;
    private String categoryName;
    private String categoryStatus; // ACTIVE / INACTIVE — set by SUPER_ADMIN, out of the chef's control

    private String subCategory;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;

    // "In stock right now" toggle — independent of approval status.
    private Boolean isAvailable;

    // PENDING (awaiting admin review) / ACTIVE (approved) / REJECTED
    private String status;

    private Integer preparationTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // When the admin approved this item (PENDING → ACTIVE) — used by the
    // frontend to sort "recently approved first" and to show a "NEW" badge.
    private LocalDateTime approvedAt;

    // Only populated when status = REJECTED, so the chef can see why.
    private String rejectionReason;
}
