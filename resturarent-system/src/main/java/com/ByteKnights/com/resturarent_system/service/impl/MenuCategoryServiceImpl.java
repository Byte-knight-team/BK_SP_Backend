package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateMenuCategoryRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateMenuCategoryRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuCategoryResponse;
import com.ByteKnights.com.resturarent_system.entity.MenuCategory;
import com.ByteKnights.com.resturarent_system.exception.DuplicateResourceException;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.MenuCategoryRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.service.MenuCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuCategoryServiceImpl implements MenuCategoryService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuCategoryServiceImpl(MenuCategoryRepository menuCategoryRepository, MenuItemRepository menuItemRepository) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getAllCategories() {
        return menuCategoryRepository.findAll()
                .stream()
                .map(category -> mapToResponse(category, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MenuCategoryResponse getCategoryById(Long id) {
        MenuCategory category = findCategoryOrThrow(id);
        return mapToResponse(category, null);
    }

    @Override
    @Transactional
    public MenuCategoryResponse createCategory(CreateMenuCategoryRequest request) {
        String normalizedName = validateAndNormalizeRequiredName(request != null ? request.getName() : null);
        String normalizedDescription = validateAndNormalizeOptionalDescription(request != null ? request.getDescription() : null);

        if (menuCategoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateResourceException("Category already exists: " + normalizedName);
        }

        MenuCategory category = MenuCategory.builder()
                .name(normalizedName)
                .description(normalizedDescription)
                .build();

        MenuCategory saved = menuCategoryRepository.save(category);
        return mapToResponse(saved, "Category created successfully");
    }

    @Override
    @Transactional
    public MenuCategoryResponse updateCategory(Long id, UpdateMenuCategoryRequest request) {
        MenuCategory category = findCategoryOrThrow(id);

        if (request != null && request.getName() != null) {
            String normalizedName = validateAndNormalizeRequiredName(request.getName());
            if (menuCategoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
                throw new DuplicateResourceException("Category already exists: " + normalizedName);
            }
            category.setName(normalizedName);
        }

        if (request != null && request.getDescription() != null) {
            category.setDescription(validateAndNormalizeOptionalDescription(request.getDescription()));
        }

        MenuCategory updated = menuCategoryRepository.save(category);
        return mapToResponse(updated, "Category updated successfully");
    }

    @Override
    @Transactional
    public MenuCategoryResponse deleteCategory(Long id) {
        MenuCategory category = findCategoryOrThrow(id);

        if (menuItemRepository.existsByCategoryIdAndIsAvailableTrue(id)) {
            throw new InvalidOperationException("Cannot delete category with available menu items");
        }

        if (menuItemRepository.existsByCategoryId(id)) {
            throw new InvalidOperationException("Cannot delete category with existing menu items");
        }

        menuCategoryRepository.delete(category);
        return mapToResponse(category, "Category deleted successfully");
    }

    private MenuCategory findCategoryOrThrow(Long id) {
        return menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + id));
    }

    private String validateAndNormalizeRequiredName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidOperationException("Category name is required");
        }

        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new InvalidOperationException("Category name must be at most " + MAX_NAME_LENGTH + " characters");
        }

        return normalizedName;
    }

    private String validateAndNormalizeOptionalDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalizedDescription = description.trim();
        if (normalizedDescription.isEmpty()) {
            return null;
        }

        if (normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidOperationException("Category description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        return normalizedDescription;
    }

    private MenuCategoryResponse mapToResponse(MenuCategory category, String message) {
        return MenuCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .message(message)
                .build();
    }
}
