package com.ByteKnights.com.resturarent_system.dto.inventory.response;

import lombok.*;

/**
 * Data Transfer Object sent to the frontend to represent an individual
 * chef request for inventory.
 *
 * Maps directly to the shape expected by the frontend's ChefRequestsSection.jsx
 * component:
 * - id → id (matches ChefRequest.id)
 * - chefName → chefName (matches ChefRequest.chefName)
 * - role → role (matches ChefRequest.chefRole)
 * - time → time (formatted from ChefRequest.createdAt, e.g., "14:20")
 * - item → item (matches ChefRequest.itemName)
 * - quantity → quantity (combined string: quantity + " " + unit, e.g., "20.0
 * kg")
 * - note → note (matches ChefRequest.chefNote)
 * - status → status (matches ChefRequest.status as String: "PENDING",
 * "APPROVED", etc.)
 * - avatarColor → avatarColor (dynamically generated hex color based on
 * chefName hash)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefRequestDTO {

    private Long id;
    private String chefName;
    private String role;
    private String time;
    private String item;
    private String quantity;
    private String note;
    private String status;
    private String avatarColor;

}
