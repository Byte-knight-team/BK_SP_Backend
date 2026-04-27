package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.admin.ApproveMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.DeleteMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.RejectMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemActionResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemResponse;

import java.util.List;

public interface MenuService {

    long getCategoryCount();
    long getSubCategoryCount();
    long getMenuItemCount();
    long getAvailableItemCount();

    MenuItemResponse createMenuItem(CreateMenuItemRequest request);

    List<MenuItemResponse> getAllMenuItems();

    List<MenuItemResponse> getPendingChefMenuItems();

    MenuItemResponse getMenuItemById(Long id);

    MenuItemResponse updateMenuItem(Long id, UpdateMenuItemRequest request);

    MenuItemActionResponse approveMenuItem(Long id, ApproveMenuItemRequest request);

    MenuItemActionResponse rejectMenuItem(Long id, RejectMenuItemRequest request);

    MenuItemActionResponse toggleMenuItemAvailability(Long id, boolean isAvailable);

    MenuItemActionResponse deleteMenuItem(Long id, DeleteMenuItemRequest request);

    List<com.ByteKnights.com.resturarent_system.dto.response.customer.MenuItemResponse> fetchCustomerMenu(Long branchId);

    List<String> getDistinctSubCategories(Long branchId, Long categoryId);

}
