package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.CreateMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.ApproveMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.DeleteMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.MenuItemActionResponse;
import com.ByteKnights.com.resturarent_system.dto.MenuItemResponse;
import com.ByteKnights.com.resturarent_system.dto.RejectMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.UpdateMenuItemRequest;

import java.util.List;

public interface MenuService {

    MenuItemResponse createMenuItem(CreateMenuItemRequest request);

    List<MenuItemResponse> getAllMenuItems();

    List<MenuItemResponse> getPendingChefMenuItems();

    MenuItemResponse getMenuItemById(Long id);

    MenuItemResponse updateMenuItem(Long id, UpdateMenuItemRequest request);

    MenuItemActionResponse approveMenuItem(Long id, ApproveMenuItemRequest request);

    MenuItemActionResponse rejectMenuItem(Long id, RejectMenuItemRequest request);

    MenuItemActionResponse toggleMenuItemAvailability(Long id, boolean isAvailable);

    MenuItemActionResponse deleteMenuItem(Long id, DeleteMenuItemRequest request);
}
