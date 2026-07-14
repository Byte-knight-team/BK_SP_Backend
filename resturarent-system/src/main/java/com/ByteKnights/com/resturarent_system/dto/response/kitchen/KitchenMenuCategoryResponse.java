package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A menu category option for the kitchen "create item" dropdown.
 *
 * Kept separate from the admin-side MenuCategoryResponse (which carries extra
 * admin-only fields like description/timestamps/message) so the kitchen menu
 * module doesn't depend on an admin-package DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitchenMenuCategoryResponse {
    private Long id;
    private String name;
    private String status; // Always "ACTIVE" here — getActiveCategories() already filters these
}
