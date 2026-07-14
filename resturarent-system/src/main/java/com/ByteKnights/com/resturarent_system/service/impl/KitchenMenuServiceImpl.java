package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateKitchenMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemIngredientRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemUpdateRequestDto;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenMenuCategoryResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenMenuEditRequestResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenMenuItemResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.MenuItemIngredientResponseDTO;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.MenuCategory;
import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemStatus;
import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequest;
import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequestStatus;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.MenuCategoryRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemUpdateRequestRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.KitchenMenuService;
import com.ByteKnights.com.resturarent_system.service.MenuItemIngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the kitchen-owned menu service.
 *
 * Deliberately independent from MenuServiceImpl (the admin-side service):
 * this class re-implements its own validation and its own MenuItem/MenuCategory
 * queries rather than delegating to her class, so nothing here can ever change
 * admin behaviour, and nothing admin does can break this file. The only things
 * shared are the JPA entities/repositories themselves (same underlying tables)
 * and, for ingredients, the already role-neutral MenuItemIngredientService.
 */
@Service
@RequiredArgsConstructor
public class KitchenMenuServiceImpl implements KitchenMenuService {

    private static final java.math.BigDecimal MAX_PRICE = new java.math.BigDecimal("99999999.99");
    private static final int MAX_PREP_MINUTES = 240;

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemUpdateRequestRepository menuItemUpdateRequestRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final MenuItemIngredientService menuItemIngredientService;

    // Resolves the calling chef's Staff profile from their JWT email. Same
    // pattern used across the codebase (e.g. ReceptionistReservationServiceImpl,
    // KitchenInventoryServiceImpl) — kept local here instead of shared so this
    // service has zero dependency on any other service class.
    private Staff resolveStaff(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return staffRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found"));
    }

    @Override
    @Transactional
    public KitchenMenuItemResponse createMenuItem(CreateKitchenMenuItemRequest request, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        Branch branch = chef.getBranch();

        MenuCategory category = menuCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found"));

        // Mirrors the admin-side rule: you can't add new items into a category
        // the super admin has switched off.
        if ("INACTIVE".equalsIgnoreCase(category.getStatus())) {
            throw new InvalidOperationException("Cannot create a menu item in an INACTIVE category");
        }

        String name = validateAndNormalizeName(request.getName());
        validatePrice(request.getPrice());
        validatePrepTime(request.getPreparationTime());

        if (menuItemRepository.existsByBranchIdAndCategoryIdAndNameIgnoreCase(
                branch.getId(), category.getId(), name)) {
            throw new InvalidOperationException("Menu item name already exists in this branch and category");
        }

        MenuItem item = MenuItem.builder()
                .branch(branch)
                .category(category)
                .subCategory(toTitleCase(request.getSubCategory().trim()))
                .name(name)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                // Unset until the chef explicitly confirms it can be cooked — admin
                // approval no longer grants this automatically either (see MenuServiceImpl).
                .isAvailable(null)
                // Every chef-created item starts PENDING — only an admin can activate it.
                .status(MenuItemStatus.PENDING)
                .preparationTime(request.getPreparationTime())
                .createdBy(chef.getUser().getId())
                .build();

        return toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenMenuItemResponse> getMyMenuItems(String userEmail) {
        Staff chef = resolveStaff(userEmail);
        return menuItemRepository.findByBranchId(chef.getBranch().getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public KitchenMenuItemResponse getMenuItemById(Long id, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        MenuItem item = findOwnedItem(id, chef);
        return toResponse(item);
    }

    @Override
    @Transactional
    public KitchenMenuItemResponse toggleAvailability(Long id, boolean isAvailable, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        MenuItem item = findOwnedItem(id, chef);

        // Only an approved, live item can be marked in/out of stock — a PENDING
        // or REJECTED item was never available to begin with.
        if (item.getStatus() != MenuItemStatus.ACTIVE) {
            throw new InvalidOperationException("Cannot toggle availability if item is not ACTIVE");
        }

        item.setIsAvailable(isAvailable);
        return toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemIngredientResponseDTO> getIngredients(Long menuItemId, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        findOwnedItem(menuItemId, chef); // branch-ownership check, result unused
        return menuItemIngredientService.getIngredients(menuItemId);
    }

    @Override
    @Transactional
    public List<MenuItemIngredientResponseDTO> saveIngredients(
            Long menuItemId, MenuItemIngredientRequestDTO request, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        findOwnedItem(menuItemId, chef); // branch-ownership check, result unused
        return menuItemIngredientService.saveIngredients(menuItemId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenMenuCategoryResponse> getActiveCategories() {
        return menuCategoryRepository.findAll()
                .stream()
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .map(c -> KitchenMenuCategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .status(c.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getMySubCategories(String userEmail, Long categoryId) {
        Staff chef = resolveStaff(userEmail);
        return menuItemRepository.findDistinctSubCategories(chef.getBranch().getId(), categoryId);
    }

    @Override
    @Transactional
    public void createEditRequest(MenuItemUpdateRequestDto request, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        MenuItem item = findOwnedItem(request.getMenuItemId(), chef);

        // Edit requests make sense once an item has been through its first
        // review — ACTIVE or INACTIVE (the branch admin can deactivate an item
        // without rejecting it; a request can ask for it to be reactivated).
        // A PENDING item hasn't had its first review yet (just edit it directly
        // by re-submitting), and a REJECTED item needs a fresh submission.
        if (item.getStatus() != MenuItemStatus.ACTIVE && item.getStatus() != MenuItemStatus.INACTIVE) {
            throw new InvalidOperationException("Only an ACTIVE or INACTIVE item can have an edit request raised against it");
        }

        // A disabled CATEGORY is the Super Admin's decision, not the branch
        // admin's — an edit request can't change that, so block it here too
        // (the item's own isAvailable flag, set by the branch admin, is fine —
        // that's exactly the kind of thing a request can ask to be reversed).
        if (item.getCategory() != null && "INACTIVE".equalsIgnoreCase(item.getCategory().getStatus())) {
            throw new InvalidOperationException(
                    "This item's category was disabled by the Super Admin — no edit request can be raised while it stays disabled");
        }

        MenuItemUpdateRequest editRequest = MenuItemUpdateRequest.builder()
                .chef(chef)
                .menuItem(item)
                .chefNote(request.getChefNote())
                .status(MenuItemUpdateRequestStatus.PENDING)
                .build();

        menuItemUpdateRequestRepository.save(editRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenMenuEditRequestResponse> getMyEditRequests(String userEmail) {
        Staff chef = resolveStaff(userEmail);

        return menuItemUpdateRequestRepository.findByChefId(chef.getId())
                .stream()
                .sorted(Comparator.comparing(MenuItemUpdateRequest::getCreatedAt).reversed())
                .map(r -> KitchenMenuEditRequestResponse.builder()
                        .id(r.getId())
                        .menuItemId(r.getMenuItem().getId())
                        .menuItemName(r.getMenuItem().getName())
                        .chefNote(r.getChefNote())
                        .adminNote(r.getAdminNote())
                        .status(r.getStatus().name())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // Loads a menu item and confirms it belongs to the calling chef's branch —
    // prevents one branch's chef from viewing/editing another branch's items.
    private MenuItem findOwnedItem(Long id, Staff chef) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));

        if (item.getBranch() == null || !item.getBranch().getId().equals(chef.getBranch().getId())) {
            throw new InvalidOperationException("Menu item access is restricted to your branch");
        }
        return item;
    }

    private String validateAndNormalizeName(String name) {
        String trimmed = name.trim();
        if (trimmed.length() < 3) {
            throw new InvalidOperationException("Name must be at least 3 characters");
        }
        return trimmed;
    }

    private void validatePrice(java.math.BigDecimal price) {
        if (price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Price must be greater than zero");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new InvalidOperationException("Price must be less than or equal to " + MAX_PRICE.toPlainString());
        }
    }

    private void validatePrepTime(Integer minutes) {
        if (minutes <= 0 || minutes > MAX_PREP_MINUTES) {
            throw new InvalidOperationException(
                    "Preparation time must be between 1 and " + MAX_PREP_MINUTES + " minutes");
        }
    }

    // "spicy chicken" -> "Spicy Chicken", same convention the admin side uses
    // for sub-category names so both sides display consistently.
    private String toTitleCase(String input) {
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(' ');
            result.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
        }
        return result.toString();
    }

    private KitchenMenuItemResponse toResponse(MenuItem item) {
        return KitchenMenuItemResponse.builder()
                .id(item.getId())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .categoryStatus(item.getCategory() != null ? item.getCategory().getStatus() : null)
                .subCategory(item.getSubCategory())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .isAvailable(item.getIsAvailable())
                .status(item.getStatus() != null ? item.getStatus().name() : null)
                .preparationTime(item.getPreparationTime())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .approvedAt(item.getApprovedAt())
                .rejectionReason(item.getRejectionReason())
                .build();
    }
}
