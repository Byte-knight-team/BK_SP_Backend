package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One of the chef's own menu-item edit requests, for the "My Requests" page.
 *
 * This is a note-based request, not a structured field diff: chefNote is free
 * text describing what should change. If the admin approves it, she applies
 * the actual change herself via her own edit screen — approving a request here
 * does not automatically modify the menu item.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitchenMenuEditRequestResponse {

    private Long id;
    private Long menuItemId;
    private String menuItemName;

    private String chefNote;
    private String adminNote; // null until the admin makes a decision

    private String status; // PENDING / APPROVED / REJECTED

    private LocalDateTime createdAt;
}
