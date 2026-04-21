package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.CreateMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.MenuItemResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateMenuItemRequest;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.MenuCategory;
import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemStatus;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuCategoryRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private static final BigDecimal MAX_MENU_ITEM_PRICE = new BigDecimal("99999999.99");
    private static final int MAX_PREPARATION_TIME_MINUTES = 240;

    private final MenuItemRepository menuItemRepository;
    private final BranchRepository branchRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    @Override
    @Transactional
    public MenuItemResponse createMenuItem(CreateMenuItemRequest request) {
        Branch branch = findBranchOrThrow(request.getBranchId());
        MenuCategory category = findCategoryOrThrow(request.getCategoryId());
        String validatedName = validateAndNormalizeRequiredName(request.getName());
        validatePriceRange(request.getPrice());
        validatePreparationTime(request.getPreparationTime());

        if (menuItemRepository.existsByBranchIdAndCategoryIdAndNameIgnoreCase(
            branch.getId(), category.getId(), validatedName)) {
            throw new InvalidOperationException("Menu item name already exists in this branch and category");
        }

        MenuItem menuItem = MenuItem.builder()
                .branch(branch)
                .category(category)
                .subCategory(request.getSubCategory())
            .name(validatedName)
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(validateAndNormalizeImageUrl(request.getImageUrl()))
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .status(parseStatus(request.getStatus(), MenuItemStatus.PENDING))
                .preparationTime(request.getPreparationTime())
                .build();

        MenuItem saved = menuItemRepository.save(menuItem);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem menuItem = findMenuItemOrThrow(id);
        return mapToResponse(menuItem);
    }

    @Override
    @Transactional
    public MenuItemResponse updateMenuItem(Long id, UpdateMenuItemRequest request) {
        MenuItem menuItem = findMenuItemOrThrow(id);

        if (request.getBranchId() != null) {
            menuItem.setBranch(findBranchOrThrow(request.getBranchId()));
        }

        if (request.getCategoryId() != null) {
            menuItem.setCategory(findCategoryOrThrow(request.getCategoryId()));
        }

        if (request.getSubCategory() != null) {
            menuItem.setSubCategory(request.getSubCategory());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            menuItem.setName(validateAndNormalizeRequiredName(request.getName()));
        }

        if (menuItemRepository.existsByBranchIdAndCategoryIdAndNameIgnoreCaseAndIdNot(
                menuItem.getBranch().getId(),
                menuItem.getCategory().getId(),
                menuItem.getName(),
                menuItem.getId())) {
            throw new InvalidOperationException("Menu item name already exists in this branch and category");
        }

        if (request.getDescription() != null) {
            menuItem.setDescription(request.getDescription());
        }

        if (request.getPrice() != null) {
            validatePriceRange(request.getPrice());
            menuItem.setPrice(request.getPrice());
        }

        if (request.getImageUrl() != null) {
            menuItem.setImageUrl(validateAndNormalizeImageUrl(request.getImageUrl()));
        }

        if (request.getIsAvailable() != null) {
            menuItem.setIsAvailable(request.getIsAvailable());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            menuItem.setStatus(parseStatus(request.getStatus(), menuItem.getStatus()));
        }

        if (request.getPreparationTime() != null) {
            validatePreparationTime(request.getPreparationTime());
            menuItem.setPreparationTime(request.getPreparationTime());
        }

        MenuItem updated = menuItemRepository.save(menuItem);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem menuItem = findMenuItemOrThrow(id);
        menuItemRepository.delete(menuItem);
    }

    private MenuItem findMenuItemOrThrow(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
    }

    private Branch findBranchOrThrow(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    }

    private MenuCategory findCategoryOrThrow(Long id) {
        return menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + id));
    }

    private MenuItemStatus parseStatus(String status, MenuItemStatus defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }

        try {
            return MenuItemStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOperationException(
                    "Invalid menu item status: " + status + ". Valid values: PENDING, APPROVED, REJECTED");
        }
    }

    private String validateAndNormalizeRequiredName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidOperationException("Name is required");
        }

        String normalizedName = name.trim();
        if (normalizedName.length() < 3) {
            throw new InvalidOperationException("Name must be at least 3 characters");
        }

        return normalizedName;
    }

    private void validatePriceRange(BigDecimal price) {
        if (price == null) {
            throw new InvalidOperationException("Price is required");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Price must be greater than zero");
        }

        if (price.compareTo(MAX_MENU_ITEM_PRICE) > 0) {
            throw new InvalidOperationException(
                    "Price must be less than or equal to " + MAX_MENU_ITEM_PRICE.toPlainString());
        }
    }

    private void validatePreparationTime(Integer preparationTime) {
        if (preparationTime == null) {
            return;
        }

        if (preparationTime <= 0) {
            throw new InvalidOperationException("Preparation time must be greater than zero");
        }

        if (preparationTime > MAX_PREPARATION_TIME_MINUTES) {
            throw new InvalidOperationException(
                    "Preparation time must be less than or equal to " + MAX_PREPARATION_TIME_MINUTES + " minutes");
        }
    }

    private String validateAndNormalizeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String normalizedImageUrl = imageUrl.trim();
        if (normalizedImageUrl.isEmpty()) {
            return null;
        }

        URI parsedUri;
        try {
            parsedUri = URI.create(normalizedImageUrl);
        } catch (IllegalArgumentException ex) {
            throw new InvalidOperationException("Image URL must be a valid HTTP/HTTPS URL");
        }

        String scheme = parsedUri.getScheme();
        if (scheme == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                || parsedUri.getHost() == null) {
            throw new InvalidOperationException("Image URL must be a valid HTTP/HTTPS URL");
        }

        return normalizedImageUrl;
    }

    private MenuItemResponse mapToResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .branchId(item.getBranch().getId())
                .branchName(item.getBranch().getName())
                .categoryId(item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .subCategory(item.getSubCategory())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .isAvailable(item.getIsAvailable())
                .status(item.getStatus().name())
                .preparationTime(item.getPreparationTime())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
