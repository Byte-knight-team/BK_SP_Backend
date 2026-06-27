package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemIngredientRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.MenuItemIngredientResponseDTO;

import java.util.List;

public interface MenuItemIngredientService {

    // Get all ingredients for a menu item
    List<MenuItemIngredientResponseDTO> getIngredients(Long menuItemId);

    // Save (replace) the full ingredient list for a menu item
    List<MenuItemIngredientResponseDTO> saveIngredients(Long menuItemId, MenuItemIngredientRequestDTO request);
}
