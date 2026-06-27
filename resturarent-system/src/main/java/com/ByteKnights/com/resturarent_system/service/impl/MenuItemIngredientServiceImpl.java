package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemIngredientRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.MenuItemIngredientResponseDTO;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.InventoryItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemIngredient;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemIngredientRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.MenuItemIngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemIngredientServiceImpl implements MenuItemIngredientService {

    private final MenuItemIngredientRepository ingredientRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final AuditLogService auditLogService;

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

        /*
         * This method replaces all ingredients for a menu item.
         * Manual audit is required because old ingredient list and new ingredient list
         * are important for governance review.
         */
        List<MenuItemIngredient> oldIngredients = ingredientRepository.findByMenuItemId(menuItemId);

        Map<String, Object> oldValues = buildMenuItemIngredientsAuditSnapshot(menuItem, oldIngredients);

        /*
         * Delete all existing ingredients first.
         * This is a replace approach, not a single-row update.
         */
        ingredientRepository.deleteByMenuItemId(menuItemId);

        /*
         * Save each new ingredient entry.
         */
        List<MenuItemIngredient> saved = request.getIngredients()
                .stream()
                .map(entry -> {
                    InventoryItem inventoryItem = inventoryItemRepository.findById(entry.getInventoryItemId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Inventory item not found with ID: " + entry.getInventoryItemId()
                            ));

                    return MenuItemIngredient.builder()
                            .menuItem(menuItem)
                            .inventoryItem(inventoryItem)
                            .quantityRequired(entry.getQuantityRequired())
                            .build();
                })
                .collect(Collectors.toList());

        List<MenuItemIngredient> savedIngredients = ingredientRepository.saveAll(saved);

        List<MenuItemIngredientResponseDTO> response = savedIngredients
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        auditLogService.logCurrentUserAction(
                AuditModule.MENU,
                AuditEventType.MENU_ITEM_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.MENU_ITEM,
                menuItem.getId(),
                null,
                "Menu item ingredients updated successfully",
                oldValues,
                buildMenuItemIngredientsAuditSnapshot(menuItem, savedIngredients)
        );

        return response;
    }

    /*
     * Maps entity to response DTO.
     */
    private MenuItemIngredientResponseDTO toResponse(MenuItemIngredient ingredient) {
        return MenuItemIngredientResponseDTO.builder()
                .id(ingredient.getId())
                .inventoryItemId(ingredient.getInventoryItem().getId())
                .inventoryItemName(ingredient.getInventoryItem().getName())
                .unit(ingredient.getInventoryItem().getUnit())
                .quantityRequired(ingredient.getQuantityRequired())
                .build();
    }

    /*
     * Builds a safe audit snapshot for menu item ingredient replacement.
     *
     * We do not store the full entity directly because MenuItem and InventoryItem
     * relationships can create large JSON or recursion problems.
     */
    private Map<String, Object> buildMenuItemIngredientsAuditSnapshot(
            MenuItem menuItem,
            List<MenuItemIngredient> ingredients
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("menuItemId", menuItem != null ? menuItem.getId() : null);
        snapshot.put("menuItemName", menuItem != null ? menuItem.getName() : null);

        List<Map<String, Object>> ingredientSnapshots = ingredients == null
                ? List.of()
                : ingredients.stream()
                .map(this::buildSingleIngredientAuditSnapshot)
                .collect(Collectors.toList());

        snapshot.put("ingredients", ingredientSnapshots);

        return snapshot;
    }

    /*
     * Builds one ingredient row for audit old/new JSON.
     */
    private Map<String, Object> buildSingleIngredientAuditSnapshot(MenuItemIngredient ingredient) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (ingredient == null) {
            return snapshot;
        }

        InventoryItem inventoryItem = ingredient.getInventoryItem();

        snapshot.put("ingredientId", ingredient.getId());
        snapshot.put("inventoryItemId", inventoryItem != null ? inventoryItem.getId() : null);
        snapshot.put("inventoryItemName", inventoryItem != null ? inventoryItem.getName() : null);
        snapshot.put("unit", inventoryItem != null ? inventoryItem.getUnit() : null);
        snapshot.put("quantityRequired", ingredient.getQuantityRequired());

        return snapshot;
    }
}