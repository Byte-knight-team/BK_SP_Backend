package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateKitchenMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemIngredientRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemUpdateRequestDto;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenMenuCategoryResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenMenuEditRequestResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenMenuItemResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.MenuItemIngredientResponseDTO;

import java.util.List;

/**
 * Kitchen-owned menu management — everything a CHEF needs to create, view and
 * (indirectly, via a request) edit their own branch's menu items.
 *
 * This is intentionally a separate service from the admin-side MenuService:
 * it re-reads the same MenuItem/MenuCategory tables but does not call into or
 * modify the admin service in any way, so admin's approve/reject/update flows
 * are completely unaffected by anything here.
 */
public interface KitchenMenuService {

    // Create a new item in the caller's branch. Always lands in PENDING,
    // unavailable, awaiting admin review — a CHEF can never self-approve.
    KitchenMenuItemResponse createMenuItem(CreateKitchenMenuItemRequest request, String userEmail);

    // All menu items in the caller's own branch, any status.
    List<KitchenMenuItemResponse> getMyMenuItems(String userEmail);

    // A single item — only if it belongs to the caller's branch.
    KitchenMenuItemResponse getMenuItemById(Long id, String userEmail);

    // Flip the "in stock now" flag. Only allowed while the item is ACTIVE.
    KitchenMenuItemResponse toggleAvailability(Long id, boolean isAvailable, String userEmail);

    // Recipe (ingredient list) for one of the caller's own items.
    List<MenuItemIngredientResponseDTO> getIngredients(Long menuItemId, String userEmail);

    List<MenuItemIngredientResponseDTO> saveIngredients(
            Long menuItemId, MenuItemIngredientRequestDTO request, String userEmail);

    // Categories a chef is allowed to pick from when creating an item —
    // ACTIVE only (an INACTIVE category can't take new items).
    List<KitchenMenuCategoryResponse> getActiveCategories();

    // Distinct sub-category names already used in the caller's branch, so the
    // create-item form can offer "pick an existing one, or type a new one".
    List<String> getMySubCategories(String userEmail, Long categoryId);

    // Ask the admin to change something on an ACTIVE item the chef doesn't
    // have direct edit rights to. Rejected if the item isn't ACTIVE yet, or
    // belongs to a different branch.
    void createEditRequest(MenuItemUpdateRequestDto request, String userEmail);

    // The caller's own edit requests (any status), newest first.
    List<KitchenMenuEditRequestResponse> getMyEditRequests(String userEmail);
}
