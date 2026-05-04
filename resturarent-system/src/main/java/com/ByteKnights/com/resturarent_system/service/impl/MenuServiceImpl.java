package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.admin.ApproveMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.DeleteMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.RejectMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemActionResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemResponse;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.MenuCategory;
import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemStatus;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuCategoryRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.MenuService;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MenuServiceImpl - Core service for managing restaurant menu items and categories.
 * 
 * This service handles all menu operations including:
 * - Creating, updating, retrieving, and deleting menu items
 * - Managing menu item approval workflows (pending -> active/rejected)
 * - Toggling menu item availability
 * - Counting menu categories, sub-categories, and items
 * - Generating distinct sub-category lists
 * 
 * Key Features:
 * - Branch-scoped access control: staff can only manage their assigned branch's menu
 * - Status-based workflow: items transition from PENDING -> ACTIVE (approved) or REJECTED
 * - Admin approval required: only ADMIN users can approve/reject pending items
 * - Immediate activation: items created by admin are automatically ACTIVE
 * - Comprehensive validation: price ranges, preparation times, and required fields
 * - Transaction management: all operations are transactional for data consistency
 * 
 * Access Control:
 * - ADMIN: Can approve/reject items, access their branch's menu
 * - CHEF: Can create/update items, view their branch's menu
 * - Admin users creating items bypass the PENDING approval workflow
 **/

@Service
public class MenuServiceImpl implements MenuService {

    private static final BigDecimal MAX_MENU_ITEM_PRICE = new BigDecimal("99999999.99");
    
    private static final int MAX_PREPARATION_TIME_MINUTES = 240;

    private final MenuItemRepository menuItemRepository;
    
    private final BranchRepository branchRepository;
    
    private final MenuCategoryRepository menuCategoryRepository;
    
    private final StaffRepository staffRepository;
    
    private final UserRepository userRepository;

    public MenuServiceImpl(MenuItemRepository menuItemRepository,
                           BranchRepository branchRepository,
                           MenuCategoryRepository menuCategoryRepository,
                           StaffRepository staffRepository,
                           UserRepository userRepository) {
        this.menuItemRepository = menuItemRepository;
        this.branchRepository = branchRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.staffRepository = staffRepository;
        this.userRepository = userRepository;
    }

    // Retrieves the count of distinct menu categories.
    @Override
    @Transactional(readOnly = true)
    public long getCategoryCount() {
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();

        if (adminBranchId != null) {
            return menuItemRepository.countDistinctCategoryByBranchId(adminBranchId);
        } else {
            return menuCategoryRepository.count();
        }
    }

    //Retrieves the count of distinct menu sub-categories.

    @Override
    @Transactional(readOnly = true)
    public long getSubCategoryCount() {
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();
        if (adminBranchId != null) {
            return menuItemRepository.countDistinctSubCategoryByBranchId(adminBranchId);
        } else {
            return menuItemRepository.countDistinctSubCategory();
        }
    }

    //Retrieves the total count of menu items.

    @Override
    @Transactional(readOnly = true)
    public long getMenuItemCount() {
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();
        if (adminBranchId != null) {
            return menuItemRepository.countByBranchId(adminBranchId);
        } else {
            return menuItemRepository.count();
        }
    }

    /**
     * Retrieves the count of available (in-stock) menu items.
     * 
     * Available items are those marked as isAvailable=true, indicating they can be
     * ordered by customers.
     **/

    @Override
    @Transactional(readOnly = true)
    public long getAvailableItemCount() {
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();
        if (adminBranchId != null) {
            return menuItemRepository.countByBranchIdAndIsAvailableTrue(adminBranchId);
        } else {
            return menuItemRepository.countByIsAvailableTrue();
        }
    }

    /**
     * Creates a new menu item in the authenticated user's branch.
     * 
     * Workflow:
     * 1. Validates all required fields (name, price, preparation time, category)
     * 2. Checks for duplicate item names within the branch-category combination
     * 3. Sets initial status based on creator role:
     *    - ADMIN: Item is immediately ACTIVE and available
     *    - CHEF: Item is PENDING and requires ADMIN approval
     * 4. Saves the item to the database
     * 
     * Validation Rules:
     * - Name: Required, minimum 3 characters, trimmed and case-sensitive unique per branch-category
     * - Price: Required, positive, maximum 99,999,999.99
     * - Preparation Time: Required, positive, maximum 240 minutes
     * - Category: Must exist in the database
     * - Sub-Category: Optional, max 50 characters, converted to title case
     * - Description: Optional, trimmed if provided
     * - Image URL: Optional, must be valid HTTP/HTTPS URL if provided
     **/
    @Override
    @Transactional
    public MenuItemResponse createMenuItem(CreateMenuItemRequest request) {
        Long branchId = resolveCurrentUserBranchId();
        Branch branch = findBranchOrThrow(branchId);
        MenuCategory category = findCategoryOrThrow(request.getCategoryId());
        String validatedName = validateAndNormalizeRequiredName(request.getName());
        validatePriceRange(request.getPrice());
        validateRequiredPreparationTime(request.getPreparationTime());

        if (menuItemRepository.existsByBranchIdAndCategoryIdAndNameIgnoreCase(
            branch.getId(), category.getId(), validatedName)) {
            throw new InvalidOperationException("Menu item name already exists in this branch and category");
        }

        Long creatorUserId = resolveCreatedByUserId();
        boolean creatorIsAdmin = isCurrentUserAdmin();

        MenuItem menuItem = MenuItem.builder()
                .branch(branch)
                .category(category)
            .subCategory(validateAndNormalizeOptionalSubCategory(request.getSubCategory()))
                .name(validatedName)
                .description(validateAndNormalizeOptionalDescription(request.getDescription()))
                .price(request.getPrice())
                .imageUrl(validateAndNormalizeImageUrl(request.getImageUrl()))
                .isAvailable(false)
                .status(MenuItemStatus.PENDING)
                .preparationTime(request.getPreparationTime())
                .createdBy(creatorUserId)
                .build();

        if (creatorIsAdmin) {
            menuItem.setStatus(MenuItemStatus.ACTIVE);
            menuItem.setIsAvailable(true);
            menuItem.setApprovedAt(LocalDateTime.now());
            menuItem.setRejectedAt(null);
            menuItem.setRejectionReason(null);
        } else {
            menuItem.setStatus(MenuItemStatus.PENDING);
            menuItem.setIsAvailable(false);
            menuItem.setApprovedAt(null);
            menuItem.setRejectedAt(null);
            menuItem.setRejectionReason(null);
        }

        MenuItem saved = menuItemRepository.save(menuItem);
        return mapToResponse(saved);
    }

    // Retrieves all menu items for the authenticated user's branch.
    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        Long branchId = resolveCurrentUserBranchId();

        List<MenuItem> items = menuItemRepository.findByBranchId(branchId);

        return items
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Retrieves all menu items in PENDING status for the authenticated admin user.
    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getPendingChefMenuItems() {
        ensureCurrentUserIsAdminForWorkflow();
        Long adminBranchId = resolveCurrentUserBranchId();

        return menuItemRepository.findByStatus(MenuItemStatus.PENDING)
                .stream()
                .filter(item -> item.getBranch() != null && adminBranchId.equals(item.getBranch().getId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Retrieves a single menu item by its ID.
    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem menuItem = findMenuItemOrThrow(id);
        enforceCurrentUserBranchAccess(menuItem.getBranch() != null ? menuItem.getBranch().getId() : null);
        return mapToResponse(menuItem);
    }

    //for customer menu - (dileepa)
    @Override
    @Transactional(readOnly = true)
    public List<com.ByteKnights.com.resturarent_system.dto.response.customer.MenuItemResponse> fetchCustomerMenu(Long branchId) {
        // ENFORCE BUSINESS RULE: Default to Branch 1 for Online customers
        // only branch 1 is doing online services
        Long targetBranchId = (branchId != null) ? branchId : 1;

        // Fetch only ACTIVE and AVAILABLE items for this specific branch
        List<MenuItem> items = menuItemRepository.findByBranchIdAndStatusAndIsAvailableTrue(
                targetBranchId, 
            MenuItemStatus.ACTIVE
        );

        // Convert the database Entities into clean DTOs for React
        return items.stream().map(item -> com.ByteKnights.com.resturarent_system.dto.response.customer.MenuItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                // Prevent NullPointerExceptions if a category was deleted
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : "Uncategorized")
                .subCategory(item.getSubCategory())
                .isAvailable(item.getIsAvailable())
                .preparationTime(item.getPreparationTime())
                .build()
        ).collect(Collectors.toList());
    }

    // Retrieves all distinct sub-category names for a given category in a branch.
    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctSubCategories(Long branchId, Long categoryId) {
        if (branchId == null) {
            branchId = resolveCurrentAdminBranchIdOrNull();
        }
        return menuItemRepository.findDistinctSubCategories(branchId, categoryId);
    }

    /**
     * Updates an existing menu item with new values.
     * 
     * Supports partial updates: only non-null fields in the request are applied.
     * 
     * Update Rules:
     * 1. Branch cannot be changed to a different branch the user doesn't have access to
     * 2. Item name must be unique within the new branch-category combination
     * 3. Status updates automatically adjust availability if not explicitly provided
     * 4. All validations (price range, preparation time) apply to new values
     **/

    @Override
    @Transactional
    public MenuItemResponse updateMenuItem(Long id, UpdateMenuItemRequest request) {
        MenuItem menuItem = findMenuItemOrThrow(id);
        enforceCurrentUserBranchAccess(menuItem.getBranch() != null ? menuItem.getBranch().getId() : null);

        if (request.getBranchId() != null) {
            enforceCurrentUserBranchAccess(request.getBranchId());
            menuItem.setBranch(findBranchOrThrow(request.getBranchId()));
        }

        if (request.getCategoryId() != null) {
            menuItem.setCategory(findCategoryOrThrow(request.getCategoryId()));
        }

        if (request.getSubCategory() != null) {
            menuItem.setSubCategory(validateAndNormalizeOptionalSubCategory(request.getSubCategory()));
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
            menuItem.setDescription(validateAndNormalizeOptionalDescription(request.getDescription()));
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
            MenuItemStatus updatedStatus = parseStatus(request.getStatus(), menuItem.getStatus());
            menuItem.setStatus(updatedStatus);

            if (request.getIsAvailable() == null) {
                if (updatedStatus == MenuItemStatus.ACTIVE) {
                    menuItem.setIsAvailable(true);
                } else {
                    menuItem.setIsAvailable(false);
                }
            }
        }

        if (request.getPreparationTime() != null) {
            validatePreparationTime(request.getPreparationTime());
            menuItem.setPreparationTime(request.getPreparationTime());
        }

        MenuItem updated = menuItemRepository.save(menuItem);
        return mapToResponse(updated);
    }

    /**
     * Approves a pending menu item, transitioning it to ACTIVE status.
     * 
     * Workflow:
     * 1. Ensures the user is an ADMIN
     * 2. Verifies the menu item is in PENDING status
     * 3. Validates the item has all required fields (name, price, category, preparation time)
     * 4. Transitions the item to ACTIVE status and marks it as available
     * 5. Records the approval timestamp
     * 6. Generates a notification payload for the item creator
     * 
     * Once approved, the item becomes immediately available for customers to order.
     **/

    @Override
    @Transactional
    public MenuItemActionResponse approveMenuItem(Long id, ApproveMenuItemRequest request) {
        ensureCurrentUserIsAdminForWorkflow();
        MenuItem menuItem = findMenuItemOrThrow(id);
        enforceCurrentUserBranchAccess(menuItem.getBranch() != null ? menuItem.getBranch().getId() : null);

        if (menuItem.getStatus() != MenuItemStatus.PENDING) {
            throw new InvalidOperationException("Item is already processed");
        }

        if (!isMenuItemValidForApproval(menuItem)) {
            throw new InvalidOperationException("Cannot approve invalid menu item");
        }

        // Approved items become ACTIVE and immediately available to customers.
        menuItem.setStatus(MenuItemStatus.ACTIVE);
        menuItem.setIsAvailable(true);
        menuItem.setApprovedAt(LocalDateTime.now());
        menuItem.setRejectedAt(null);
        menuItem.setRejectionReason(null);
        menuItemRepository.save(menuItem);

        return buildActionResponse(
            "ACTIVE",
            menuItem,
            "Item approved and activated. Notification sent to chef.",
            buildMenuDecisionNotificationPayload("APPROVED", menuItem, null)
        );
    }

    /**
     * Rejects a pending menu item, transitioning it to REJECTED status.
     * 
     * Workflow:
     * 1. Ensures the user is an ADMIN
     * 2. Verifies the menu item is in PENDING status
     * 3. Validates that a rejection reason is provided
     * 4. Transitions the item to REJECTED status and marks it as unavailable
     * 5. Records the rejection timestamp and reason
     * 6. Generates a notification payload with the rejection reason for the item creator
     * 
     * Once rejected, the item cannot be ordered by customers and requires re-submission.
     **/
    @Override
    @Transactional
    public MenuItemActionResponse rejectMenuItem(Long id, RejectMenuItemRequest request) {
        ensureCurrentUserIsAdminForWorkflow();
        MenuItem menuItem = findMenuItemOrThrow(id);
        enforceCurrentUserBranchAccess(menuItem.getBranch() != null ? menuItem.getBranch().getId() : null);

        if (menuItem.getStatus() != MenuItemStatus.PENDING) {
            throw new InvalidOperationException("Item is already processed");
        }

        String rejectionReason = request != null ? request.getRejectionReason() : null;
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new InvalidOperationException("Rejection reason is required");
        }

        menuItem.setStatus(MenuItemStatus.REJECTED);
        menuItem.setIsAvailable(false);
        menuItem.setRejectedAt(LocalDateTime.now());
        menuItem.setRejectionReason(rejectionReason.trim());
        menuItem.setApprovedAt(null);
        menuItemRepository.save(menuItem);

        return buildActionResponse(
            "REJECTED",
            menuItem,
            "Item rejected: " + rejectionReason.trim() + ". Notification sent to chef.",
            buildMenuDecisionNotificationPayload("REJECTED", menuItem, rejectionReason.trim())
        );
    }

    /**
     * Toggles the availability status of a menu item.
     * 
     * Used to temporarily mark items as unavailable (out of stock) without removing them
     * from the menu or changing their ACTIVE status.
     * 
     * Precondition:
     * - Item must be in ACTIVE status (only ACTIVE items can be toggled)
     **/

    @Override
    @Transactional
    public MenuItemActionResponse toggleMenuItemAvailability(Long id, boolean isAvailable) {
        MenuItem menuItem = findMenuItemOrThrow(id);
        enforceCurrentUserBranchAccess(menuItem.getBranch() != null ? menuItem.getBranch().getId() : null);

        if (menuItem.getStatus() != MenuItemStatus.ACTIVE) {
            throw new InvalidOperationException("Cannot toggle availability if item is not ACTIVE");
        }

        menuItem.setIsAvailable(isAvailable);
        menuItemRepository.save(menuItem);
        return buildActionResponse(
            isAvailable ? "AVAILABLE" : "UNAVAILABLE",
            menuItem,
            "Availability updated");
    }

    /**
     * Permanently deletes a menu item from the system.
     * 
     * Precondition:
     * - Item cannot be in ACTIVE status (only inactive items can be deleted)
     * - This prevents accidental deletion of items currently available to customers
     **/
    
    @Override
    @Transactional
    public MenuItemActionResponse deleteMenuItem(Long id, DeleteMenuItemRequest request) {
        MenuItem menuItem = findMenuItemOrThrow(id);
        enforceCurrentUserBranchAccess(menuItem.getBranch() != null ? menuItem.getBranch().getId() : null);

        if (menuItem.getStatus() == MenuItemStatus.ACTIVE) {
            throw new InvalidOperationException("Cannot delete active item");
        }

        MenuItemActionResponse response = buildActionResponse("DELETED", menuItem, "Menu item deleted successfully");
        menuItemRepository.delete(menuItem);
        return response;
    }

    // Helper method: Retrieves a menu item by ID or throws an exception if not found.

    private MenuItem findMenuItemOrThrow(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
    }

    // Helper method: Retrieves a branch by ID or throws an exception if not found.

    private Branch findBranchOrThrow(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    }

    // Helper method: Retrieves a menu category by ID or throws an exception if not found.

    private MenuCategory findCategoryOrThrow(Long id) {
        return menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + id));
    }

    /**
     * Helper method: Parses a status string into MenuItemStatus enum.
     * 
     * Handles case-insensitive conversion and provides a default fallback.
     * Valid status values: PENDING, ACTIVE, INACTIVE, REJECTED
    **/

    private MenuItemStatus parseStatus(String status, MenuItemStatus defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }

        try {
            return MenuItemStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOperationException(
                    "Invalid menu item status: " + status + ". Valid values: PENDING, ACTIVE, INACTIVE, REJECTED");
        }
    }

    /**
     * Helper method: Validates and normalizes a required menu item name.
     * 
     * Validation Rules:
     * - Cannot be null or blank
     * - Minimum 3 characters after trimming
     * - Whitespace is trimmed but not otherwise modified
    **/ 

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

    /**
     * Helper method: Validates menu item price is within acceptable range.
     * 
     * Validation Rules:
     * - Price cannot be null
     * - Price must be greater than zero
     * - Price cannot exceed MAX_MENU_ITEM_PRICE (99,999,999.99)
    **/

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

    /**
     * Helper method: Validates optional preparation time is within acceptable range.
     * 
     * Validation Rules:
     * - If provided, must be greater than zero
     * - Cannot exceed MAX_PREPARATION_TIME_MINUTES (240 minutes)
     * - Null is acceptable (optional field)
    **/

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

    /**
     * Helper method: Validates that preparation time is required and within range.
     * 
     * Similar to validatePreparationTime but enforces that the value is required
     * (cannot be null). Used during item creation.
    **/

    private void validateRequiredPreparationTime(Integer preparationTime) {
        if (preparationTime == null) {
            throw new InvalidOperationException("Preparation time is required");
        }
        validatePreparationTime(preparationTime);
    }

    /**
     * Helper method: Validates and normalizes an image URL.
     * 
     * Validation Rules:
     * - Null values are acceptable (optional field) and return null
     * - Must be a valid URI with HTTP or HTTPS scheme
     * - Must have a valid hostname
     * - Whitespace is trimmed
    **/

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

    /**
     * Helper method: Validates and normalizes an optional description.
     * 
     * Validation Rules:
     * - Null values are acceptable (optional field) and return null
     * - Empty strings after trimming return null
     * - Whitespace is trimmed
     * - No maximum length enforced
    **/

    private String validateAndNormalizeOptionalDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalizedDescription = description.trim();
        if (normalizedDescription.isEmpty()) {
            return null;
        }

        return normalizedDescription;
    }

    /**
     * Helper method: Validates and normalizes an optional sub-category name.
     * 
     * Validation Rules:
     * - Null values are acceptable (optional field) and return null
     * - Cannot be blank if provided
     * - Maximum 50 characters
     * - Converted to title case (e.g., "spicy chicken" -> "Spicy Chicken")
    **/

    private String validateAndNormalizeOptionalSubCategory(String subCategory) {
        if (subCategory == null) {
            return null;
        }

        String normalizedSubCategory = subCategory.trim();
        if (normalizedSubCategory.isEmpty()) {
            throw new InvalidOperationException("Sub category must not be blank");
        }

        if (normalizedSubCategory.length() >= 50) {
            throw new InvalidOperationException("Sub category must be less than 50 characters");
        }

        // Convert to title case: lowercase first, then capitalize first letter of each word
        normalizedSubCategory = toTitleCase(normalizedSubCategory);

        return normalizedSubCategory;
    }

    /**
     * Helper method: Converts a string to title case.
     * 
     * Example: "spicy chicken" -> "Spicy Chicken"
     * Words are split by whitespace and first letter of each word is capitalized.
    **/

    private String toTitleCase(String input) {
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(words[i].charAt(0)));
            result.append(words[i].substring(1));
        }
        return result.toString();
    }

    /**
     * Helper method: Validates that a menu item has all required fields for approval.
     * 
     * Pre-approval Validation:
     * - Name: Must not be null or blank
     * - Price: Must be greater than zero
     * - Category: Must not be null
     * - Preparation Time: Must be greater than zero
    **/

    private boolean isMenuItemValidForApproval(MenuItem menuItem) {
        boolean hasValidName = menuItem.getName() != null && !menuItem.getName().isBlank();
        boolean hasValidPrice = menuItem.getPrice() != null && menuItem.getPrice().compareTo(BigDecimal.ZERO) > 0;
        boolean hasValidCategory = menuItem.getCategory() != null;
        boolean hasValidPreparationTime = menuItem.getPreparationTime() != null && menuItem.getPreparationTime() > 0;
        return hasValidName && hasValidPrice && hasValidCategory && hasValidPreparationTime;
    }

    // Helper method: Builds a MenuItemActionResponse without notification payload.

    private MenuItemActionResponse buildActionResponse(String type, MenuItem menuItem, String message) {
        return buildActionResponse(type, menuItem, message, null);
    }

    /**
     * Helper method: Builds a complete MenuItemActionResponse with all details.
     * 
     * Creates a response object containing the action result, menu item details,
     * message, and optional notification payload for real-time notification systems.
    **/

    private MenuItemActionResponse buildActionResponse(
            String type,
            MenuItem menuItem,
            String message,
            Map<String, Object> notificationPayload
    ) {
        return MenuItemActionResponse.builder()
                .type(type)
                .menuItemId(menuItem.getId())
                .menuItemName(menuItem.getName())
                .message(message)
                .notificationPayload(notificationPayload)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Helper method: Resolves the authenticated user's ID from the security context.
     * 
     * Extracts the user ID from the JWT principal in the current authentication.
     * Used to record who created or modified a menu item.
     * */

    private Long resolveCreatedByUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
            if (principal.getUser() != null && principal.getUser().getId() != null) {
                return principal.getUser().getId();
            }
        }

        throw new InvalidOperationException("Authenticated user not found");
    }

    /**
     * Helper method: Checks if the authenticated user has ADMIN role.
     * 
     * Searches the user's granted authorities for the ROLE_ADMIN authority.
     * */

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority));
    }

    /**
     * Helper method: Ensures the current user is an ADMIN for approval workflow.
     * 
     * Used as a precondition check for admin-only operations like approve/reject.
     * */

    private void ensureCurrentUserIsAdminForWorkflow() {
        if (!isCurrentUserAdmin()) {
            throw new InvalidOperationException("Only ADMIN can approve/reject pending menu items");
        }
    }

    /**
     * Helper method: Enforces that the authenticated user has access to a specific branch.
     * 
     * Verifies that the target branch matches the user's assigned branch.
     * Prevents users from accessing or modifying items in other branches.
     * */

    private void enforceCurrentUserBranchAccess(Long targetBranchId) {
        Long userBranchId = resolveCurrentUserBranchId();
        if (targetBranchId == null || !userBranchId.equals(targetBranchId)) {
            throw new InvalidOperationException("Menu item access is restricted to your branch");
        }
    }

    /**
     * Helper method: Resolves the authenticated user's assigned branch ID.
     * 
     * Extracts the branch ID from the user's staff profile. Only ADMIN and CHEF
     * roles are supported for branch-scoped operations.
     * 
     * Access Control:
     * - Requires ROLE_ADMIN or ROLE_CHEF
     * - User must have a staff profile with an assigned branch
     * */

    private Long resolveCurrentUserBranchId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getAuthorities() == null) {
            throw new InvalidOperationException("Authenticated user not found");
        }

        boolean isScopedRole = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_CHEF".equals(a.getAuthority()));

        if (!isScopedRole) {
            throw new InvalidOperationException("Only ADMIN or CHEF can perform branch-scoped menu operations");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof JwtUserPrincipal jwtUser)
                || jwtUser.getUser() == null
                || jwtUser.getUser().getId() == null) {
            throw new InvalidOperationException("Authenticated user not found");
        }

        return staffRepository.findByUserId(jwtUser.getUser().getId())
                .map(staff -> staff.getBranch().getId())
                .orElseThrow(() -> new InvalidOperationException("Staff profile not found for authenticated user"));
    }

    /**
     * Helper method: Resolves the authenticated admin user's assigned branch ID, or returns null.
     * 
     * Similar to resolveCurrentUserBranchId() but is more lenient:
     * - Returns null if user is not an ADMIN (instead of throwing exception)
     * - Used for optional branch filtering in count/list operations
     * */

    private Long resolveCurrentAdminBranchIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof JwtUserPrincipal jwtUser)
                || jwtUser.getUser() == null
                || jwtUser.getUser().getId() == null) {
            throw new InvalidOperationException("Authenticated ADMIN user not found");
        }

        return staffRepository.findByUserId(jwtUser.getUser().getId())
                .map(staff -> staff.getBranch().getId())
                .orElseThrow(() -> new InvalidOperationException("Admin staff profile not found"));
    }

    /**
     * Helper method: Builds a notification payload for menu approval/rejection decisions.
     * 
     * Creates a map of all relevant information about a menu item decision that should
     * be sent to the item creator (chef) via a notification system (WebSocket, email, etc.).
     * 
     * Payload includes:
     * - Decision: "APPROVED" or "REJECTED"
     * - Menu item details: ID, name, category, branch, creation metadata
     * - Timestamps: when approved or rejected
     * - Rejection reason: only populated if decision is "REJECTED"
     * */

    private Map<String, Object> buildMenuDecisionNotificationPayload(
            String decision,
            MenuItem menuItem,
            String rejectionReason
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("decision", decision);
        payload.put("menuItemId", menuItem.getId());
        payload.put("menuItemName", menuItem.getName());
        payload.put("branchId", menuItem.getBranch() != null ? menuItem.getBranch().getId() : null);
        payload.put("categoryId", menuItem.getCategory() != null ? menuItem.getCategory().getId() : null);
        payload.put("createdBy", menuItem.getCreatedBy());
        payload.put("status", menuItem.getStatus() != null ? menuItem.getStatus().name() : null);
        payload.put("approvedAt", menuItem.getApprovedAt());
        payload.put("rejectedAt", menuItem.getRejectedAt());
        payload.put("rejectionReason", rejectionReason);
        payload.put("timestamp", LocalDateTime.now());
        return payload;
    }

    /**
     * Helper method: Converts a MenuItem entity to a MenuItemResponse DTO.
     * 
     * Transforms the JPA entity into a response object suitable for API responses.
     * Safely handles null relationships (branch, category) to prevent null pointer exceptions.
     * */

    private MenuItemResponse mapToResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .branchId(item.getBranch() != null ? item.getBranch().getId() : null)
                .branchName(item.getBranch() != null ? item.getBranch().getName() : null)
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .subCategory(item.getSubCategory())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .isAvailable(item.getIsAvailable())
                .status(item.getStatus() != null ? item.getStatus().name() : null)
                .preparationTime(item.getPreparationTime())
                .createdAt(item.getCreatedAt())
                .build();
    }
}