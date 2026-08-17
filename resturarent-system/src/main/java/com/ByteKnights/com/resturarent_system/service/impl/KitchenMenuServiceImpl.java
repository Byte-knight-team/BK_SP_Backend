package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateKitchenMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemIngredientRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemUpdateRequestDto;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenMenuCategoryResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenMenuEditRequestResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenMenuItemResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.MenuItemIngredientResponseDTO;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
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
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.KitchenMenuService;
import com.ByteKnights.com.resturarent_system.service.MenuItemIngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final AuditLogService auditLogService;

    private Staff resolveStaff(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return staffRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found"));
    }

    @Override
    @Auditable(
            module = AuditModule.MENU,
            eventType = AuditEventType.MENU_ITEM_CREATED,
            targetType = AuditTargetType.MENU_ITEM,
            description = "Kitchen menu item created successfully",
            captureResultAsNewValue = false
    )
    @Transactional
    public KitchenMenuItemResponse createMenuItem(CreateKitchenMenuItemRequest request, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        Branch branch = chef.getBranch();

        MenuCategory category = menuCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found"));

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
                .isAvailable(null)
                .status(MenuItemStatus.PENDING)
                .preparationTime(request.getPreparationTime())
                .createdBy(chef.getUser().getId())
                .build();

        /*
         * AOP audit is used because this is a simple create action.
         * The created item starts as PENDING and admin approval/rejection is audited separately.
         */
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
    @CacheEvict(value = "crave:menu:customer", allEntries = true)
    public KitchenMenuItemResponse toggleAvailability(Long id, boolean isAvailable, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        MenuItem item = findOwnedItem(id, chef);

        if (item.getStatus() != MenuItemStatus.ACTIVE) {
            throw new InvalidOperationException("Cannot toggle availability if item is not ACTIVE");
        }

        Map<String, Object> oldValues = buildMenuItemAuditSnapshot(item);

        item.setIsAvailable(isAvailable);
        MenuItem savedItem = menuItemRepository.save(item);

        auditLogService.logCurrentUserAction(
                AuditModule.MENU,
                AuditEventType.MENU_ITEM_AVAILABILITY_CHANGED,
                AuditStatus.SUCCESS,
                isAvailable ? AuditSeverity.INFO : AuditSeverity.WARN,
                AuditTargetType.MENU_ITEM,
                savedItem.getId(),
                getMenuItemBranchId(savedItem),
                "Kitchen menu item availability changed by chef",
                oldValues,
                buildMenuItemAuditSnapshot(savedItem)
        );

        return toResponse(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemIngredientResponseDTO> getIngredients(Long menuItemId, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        findOwnedItem(menuItemId, chef);

        return menuItemIngredientService.getIngredients(menuItemId);
    }

    @Override
    @Transactional
    public List<MenuItemIngredientResponseDTO> saveIngredients(
            Long menuItemId, MenuItemIngredientRequestDTO request, String userEmail) {
        Staff chef = resolveStaff(userEmail);
        findOwnedItem(menuItemId, chef);

        /*
         * No audit here to avoid duplicate logs.
         * MenuItemIngredientService.saveIngredients(...) already performs manual audit.
         */
        return menuItemIngredientService.saveIngredients(menuItemId, request);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "crave:menu:categories", key = "'active'")
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

        if (item.getStatus() != MenuItemStatus.ACTIVE && item.getStatus() != MenuItemStatus.INACTIVE) {
            throw new InvalidOperationException("Only an ACTIVE or INACTIVE item can have an edit request raised against it");
        }

        if (item.getCategory() != null && "INACTIVE".equalsIgnoreCase(item.getCategory().getStatus())) {
            throw new InvalidOperationException(
                    "This item's category was disabled by the Super Admin — no edit request can be raised while it stays disabled"
            );
        }

        MenuItemUpdateRequest editRequest = MenuItemUpdateRequest.builder()
                .chef(chef)
                .menuItem(item)
                .chefNote(request.getChefNote())
                .status(MenuItemUpdateRequestStatus.PENDING)
                .build();

        MenuItemUpdateRequest savedRequest = menuItemUpdateRequestRepository.save(editRequest);

        auditLogService.logCurrentUserAction(
                AuditModule.MENU,
                AuditEventType.MENU_ITEM_UPDATE_REQUEST_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.MENU_ITEM_UPDATE_REQUEST,
                savedRequest.getId(),
                getMenuItemBranchId(item),
                "Kitchen menu item edit request created by chef",
                null,
                buildEditRequestAuditSnapshot(savedRequest)
        );
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
                    "Preparation time must be between 1 and " + MAX_PREP_MINUTES + " minutes"
            );
        }
    }

    private String toTitleCase(String input) {
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(' ');
            }

            result.append(Character.toUpperCase(words[i].charAt(0)))
                    .append(words[i].substring(1));
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

    private Map<String, Object> buildMenuItemAuditSnapshot(MenuItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (item == null) {
            return snapshot;
        }

        snapshot.put("menuItemId", item.getId());
        snapshot.put("branchId", getMenuItemBranchId(item));
        snapshot.put("categoryId", item.getCategory() != null ? item.getCategory().getId() : null);
        snapshot.put("categoryName", item.getCategory() != null ? item.getCategory().getName() : null);
        snapshot.put("subCategory", item.getSubCategory());
        snapshot.put("name", item.getName());
        snapshot.put("price", item.getPrice());
        snapshot.put("available", item.getIsAvailable());
        snapshot.put("status", item.getStatus() != null ? item.getStatus().name() : null);
        snapshot.put("preparationTime", item.getPreparationTime());
        snapshot.put("updatedAt", item.getUpdatedAt());

        return snapshot;
    }

    private Map<String, Object> buildEditRequestAuditSnapshot(MenuItemUpdateRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (request == null) {
            return snapshot;
        }

        Staff chef = request.getChef();
        MenuItem item = request.getMenuItem();

        snapshot.put("requestId", request.getId());
        snapshot.put("status", request.getStatus() != null ? request.getStatus().name() : null);
        snapshot.put("chefNote", request.getChefNote());
        snapshot.put("adminNote", request.getAdminNote());
        snapshot.put("createdAt", request.getCreatedAt());

        snapshot.put("chefId", chef != null ? chef.getId() : null);
        snapshot.put("chefName", buildStaffName(chef));

        snapshot.put("menuItemId", item != null ? item.getId() : null);
        snapshot.put("menuItemName", item != null ? item.getName() : null);
        snapshot.put("branchId", getMenuItemBranchId(item));

        return snapshot;
    }

    private Long getMenuItemBranchId(MenuItem item) {
        if (item == null || item.getBranch() == null) {
            return null;
        }

        return item.getBranch().getId();
    }

    private String buildStaffName(Staff staff) {
        if (staff == null) {
            return null;
        }

        if (staff.getUser() != null && staff.getUser().getFullName() != null) {
            return staff.getUser().getFullName();
        }

        String firstName = staff.getFirstName() != null ? staff.getFirstName() : "";
        String lastName = staff.getLastName() != null ? staff.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? null : fullName;
    }
}