package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.request.admin.ApproveMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.DeleteMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.RejectMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuCategoryResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemActionResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemResponse;
import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.service.MenuCategoryService;
import com.ByteKnights.com.resturarent_system.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/menu")
@CrossOrigin
public class MenuController {

    /**
     * Controller responsible for menu-related operations.
     *
     * Exposes endpoints used by administrators and chefs to manage menu
     * categories and items (create, update, approve/reject, toggle
     * availability, delete, and various counts). Customer-facing endpoints
     * are present below and intentionally not documented here per request.
     */

    private final MenuService menuService;
    private final MenuCategoryService menuCategoryService;

    public MenuController(MenuService menuService, MenuCategoryService menuCategoryService) {
        this.menuService = menuService;
        this.menuCategoryService = menuCategoryService;
    }

    @GetMapping("/pending-chef-items")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('VIEW_PENDING_ITEMS')")
    public ResponseEntity<List<MenuItemResponse>> getPendingChefMenuItems() {
        // Retrieve items submitted by chefs that are awaiting admin review
        List<MenuItemResponse> menuItems = menuService.getPendingChefMenuItems();
        return ResponseEntity.ok(menuItems);
    }

    @GetMapping("/categories/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('VIEW_CATEGORY_COUNT')")
    public ResponseEntity<Long> getCategoriesCount() {
        // Return the total number of menu categories in the system
        long count = menuService.getCategoryCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/subcategories/count")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('VIEW_SUBCATEGORY_COUNT')")
    public ResponseEntity<Long> getSubCategoriesCount() {
        // Return the total number of distinct subcategories
        long count = menuService.getSubCategoryCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('VIEW_ITEMS_COUNT')")
    public ResponseEntity<Long> getMenuItemsCount() {
        // Return the total number of menu items in the system
        long count = menuService.getMenuItemCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/available/count")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('VIEW_AVAILABLE_ITEMS_COUNT')")
    public ResponseEntity<Long> getAvailableItemsCount() {
        // Return the count of items currently marked as available
        long count = menuService.getAvailableItemCount();
        return ResponseEntity.ok(count);
    }

    /*@GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF','SUPER_ADMIN') or hasAuthority('VIEW_CATEGORIES")
    public ResponseEntity<List<MenuCategoryResponse>> getMenuCategories() {
        List<MenuCategoryResponse> categories = menuCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }*/

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CHEF') or hasAuthority('VIEW_ALL_ITEMS')")
    public ResponseEntity<List<MenuItemResponse>> getAllMenuItems() {
        // Retrieve all menu items (regardless of availability/approval)
        List<MenuItemResponse> menuItems = menuService.getAllMenuItems();
        return ResponseEntity.ok(menuItems);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF') or hasAuthority('VIEW_ITEM_BY_ID')")
    public ResponseEntity<MenuItemResponse> getMenuItemById(@PathVariable Long id) {
        // Get a single menu item by its numeric ID
        MenuItemResponse menuItem = menuService.getMenuItemById(id);
        return ResponseEntity.ok(menuItem);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CHEF','ADMIN') or hasAuthority('CREATE_ITEM')")
    public ResponseEntity<MenuItemResponse> createMenuItem(@Valid @RequestBody CreateMenuItemRequest request) {
        // Create a new menu item (accessible to chefs and admins)
        MenuItemResponse created = menuService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('UPDATE_ITEM')")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        // Update an existing menu item identified by ID
        MenuItemResponse updated = menuService.updateMenuItem(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id:\\d+}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('APPROVE_PENDING_ITEM')")
    public ResponseEntity<MenuItemActionResponse> approveMenuItem(@PathVariable Long id, @Valid @RequestBody ApproveMenuItemRequest request) {
        // Approve a pending menu item (admin only)
        MenuItemActionResponse response = menuService.approveMenuItem(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id:\\d+}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('REJECT_PENDING_ITEM')")
    public ResponseEntity<MenuItemActionResponse> rejectMenuItem(@PathVariable Long id, @Valid @RequestBody RejectMenuItemRequest request) {
        // Reject a pending menu item with a reason (admin only)
        MenuItemActionResponse response = menuService.rejectMenuItem(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id:\\d+}/availability")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('TOGGLE_ITEM_AVAILABILITY')")
    public ResponseEntity<MenuItemActionResponse> toggleMenuItemAvailability(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        // Toggle the availability flag for a menu item. Expects JSON payload: { "isAvailable": true|false }
        Boolean isAvailable = payload != null ? payload.get("isAvailable") : null;
        if (isAvailable == null) {
            // Bad request if the required field is missing
            return ResponseEntity.badRequest().body(MenuItemActionResponse.builder()
                    .type("BAD_REQUEST")
                    .menuItemId(id)
                    .menuItemName(null)
                    .message("isAvailable is required")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        MenuItemActionResponse response = menuService.toggleMenuItemAvailability(id, isAvailable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('DELETE_ITEM')")
    public ResponseEntity<MenuItemActionResponse> deleteMenuItem(@PathVariable Long id, @Valid @RequestBody DeleteMenuItemRequest request) {
        // Delete a menu item (performs any audit/authorization checks in service)
        MenuItemActionResponse response = menuService.deleteMenuItem(id, request);
        return ResponseEntity.ok(response);
    }

    //CUSTOMER ENDPOINT (Only active items)
    @GetMapping("/customer")
    public ResponseEntity<ApiResponse<List<com.ByteKnights.com.resturarent_system.dto.response.customer.MenuItemResponse>>> getMenu(
            @RequestParam(required = false) Long branchId) {

        List<com.ByteKnights.com.resturarent_system.dto.response.customer.MenuItemResponse> menuItems = menuService.fetchCustomerMenu(branchId);

        return ResponseEntity.ok(ApiResponse.success("Menu fetched successfully", menuItems));
    }

    /**
     * Retrieve a list of distinct subcategory names.
     *
     * Optional query parameters filter the result set:
     * - `branchId`: limits subcategories to a specific branch
     * - `categoryId`: limits subcategories to a specific category
     *
     * Returns unique subcategory names currently registered for the
     * (optional) branch/category combination.
     */
    @GetMapping("/subcategories")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('VIEW_ALL_SUBCATEGORIES')")
    public ResponseEntity<List<String>> getDistinctSubCategories(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long categoryId) {
        List<String> subCategories = menuService.getDistinctSubCategories(branchId, categoryId);
        return ResponseEntity.ok(subCategories);
    }
}
