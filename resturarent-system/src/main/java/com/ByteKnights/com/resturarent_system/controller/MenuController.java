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

    private final MenuService menuService;
    private final MenuCategoryService menuCategoryService;

    public MenuController(MenuService menuService, MenuCategoryService menuCategoryService) {
        this.menuService = menuService;
        this.menuCategoryService = menuCategoryService;
    }

    @GetMapping("/pending-chef-items")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<MenuItemResponse>> getPendingChefMenuItems() {
        List<MenuItemResponse> menuItems = menuService.getPendingChefMenuItems();
        return ResponseEntity.ok(menuItems);
    }

    @GetMapping("/categories/count")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Long> getCategoriesCount() {
        long count = menuService.getCategoryCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/subcategories/count")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Long> getSubCategoriesCount() {
        long count = menuService.getSubCategoryCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Long> getMenuItemsCount() {
        long count = menuService.getMenuItemCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/available/count")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Long> getAvailableItemsCount() {
        long count = menuService.getAvailableItemCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<MenuCategoryResponse>> getMenuCategories() {
        List<MenuCategoryResponse> categories = menuCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<MenuItemResponse> getMenuItemById(@PathVariable Long id) {
        MenuItemResponse menuItem = menuService.getMenuItemById(id);
        return ResponseEntity.ok(menuItem);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CHEF','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MenuItemResponse> createMenuItem(@Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemResponse created = menuService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        MenuItemResponse updated = menuService.updateMenuItem(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id:\\d+}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MenuItemActionResponse> approveMenuItem(@PathVariable Long id, @Valid @RequestBody ApproveMenuItemRequest request) {
        MenuItemActionResponse response = menuService.approveMenuItem(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id:\\d+}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MenuItemActionResponse> rejectMenuItem(@PathVariable Long id, @Valid @RequestBody RejectMenuItemRequest request) {
        MenuItemActionResponse response = menuService.rejectMenuItem(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id:\\d+}/availability")
    public ResponseEntity<MenuItemActionResponse> toggleMenuItemAvailability(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        Boolean isAvailable = payload != null ? payload.get("isAvailable") : null;
        if (isAvailable == null) {
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
    public ResponseEntity<MenuItemActionResponse> deleteMenuItem(@PathVariable Long id, @Valid @RequestBody DeleteMenuItemRequest request) {
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

    @GetMapping("/subcategories")
    public ResponseEntity<List<String>> getDistinctSubCategories(
            @RequestParam Long branchId,
            @RequestParam Long categoryId) {
        List<String> subCategories = menuService.getDistinctSubCategories(branchId, categoryId);
        return ResponseEntity.ok(subCategories);
    }
}
