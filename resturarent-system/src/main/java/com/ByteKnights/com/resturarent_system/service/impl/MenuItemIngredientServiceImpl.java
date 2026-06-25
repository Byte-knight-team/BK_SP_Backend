package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemIngredientRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.MenuItemIngredientResponseDTO;
import com.ByteKnights.com.resturarent_system.entity.InventoryItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemIngredient;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemIngredientRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.service.MenuItemIngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemIngredientServiceImpl implements MenuItemIngredientService {

    private final MenuItemIngredientRepository ingredientRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    public List<MenuItemIngredientResponseDTO> getIngredients(Long menuItemId) {
        menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found with ID: " + menuItemId));

        return ingredientRepository.findByMenuItemId(menuItemId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<MenuItemIngredientResponseDTO> saveIngredients(Long menuItemId, MenuItemIngredientRequestDTO request) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found with ID: " + menuItemId));

        // Delete all existing ingredients first (replace approach)
        ingredientRepository.deleteByMenuItemId(menuItemId);

        // Save each new ingredient entry
        List<MenuItemIngredient> saved = request.getIngredients().stream()
                .map(entry -> {
                    InventoryItem inventoryItem = inventoryItemRepository.findById(entry.getInventoryItemId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Inventory item not found with ID: " + entry.getInventoryItemId()));

                    return MenuItemIngredient.builder()
                            .menuItem(menuItem)
                            .inventoryItem(inventoryItem)
                            .quantityRequired(entry.getQuantityRequired())
                            .build();
                })
                .collect(Collectors.toList());

        return ingredientRepository.saveAll(saved)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Map entity to response DTO
    private MenuItemIngredientResponseDTO toResponse(MenuItemIngredient ingredient) {
        return MenuItemIngredientResponseDTO.builder()
                .id(ingredient.getId())
                .inventoryItemId(ingredient.getInventoryItem().getId())
                .inventoryItemName(ingredient.getInventoryItem().getName())
                .unit(ingredient.getInventoryItem().getUnit())
                .quantityRequired(ingredient.getQuantityRequired())
                .build();
    }
}
