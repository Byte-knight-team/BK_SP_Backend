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
                .description(request.getDescription())
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

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        Long branchId = resolveCurrentUserBranchId();

        List<MenuItem> items;
        if (isCurrentUserAdmin()) {
            items = menuItemRepository.findByBranchId(branchId);
        } else {
            items = menuItemRepository.findByBranchIdAndStatusAndIsAvailableTrue(branchId, MenuItemStatus.ACTIVE);
        }

        return items
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

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

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctSubCategories(Long branchId, Long categoryId) {
        if (branchId == null) {
            branchId = resolveCurrentAdminBranchIdOrNull();
        }
        return menuItemRepository.findDistinctSubCategories(branchId, categoryId);
    }

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
            "Item approved and activated",
            buildMenuDecisionNotificationPayload("APPROVED", menuItem, null)
        );
    }

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
            "Item rejected: " + rejectionReason.trim(),
            buildMenuDecisionNotificationPayload("REJECTED", menuItem, rejectionReason.trim())
        );
    }

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

        // Backward compatibility for old API clients that still send APPROVED.
        if ("APPROVED".equalsIgnoreCase(status)) {
            return MenuItemStatus.ACTIVE;
        }

        try {
            return MenuItemStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOperationException(
                    "Invalid menu item status: " + status + ". Valid values: DRAFT, PENDING, ACTIVE, REJECTED");
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

    private void validateRequiredPreparationTime(Integer preparationTime) {
        if (preparationTime == null) {
            throw new InvalidOperationException("Preparation time is required");
        }
        validatePreparationTime(preparationTime);
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

    private boolean isMenuItemValidForApproval(MenuItem menuItem) {
        boolean hasValidName = menuItem.getName() != null && !menuItem.getName().isBlank();
        boolean hasValidPrice = menuItem.getPrice() != null && menuItem.getPrice().compareTo(BigDecimal.ZERO) > 0;
        boolean hasValidCategory = menuItem.getCategory() != null;
        boolean hasValidPreparationTime = menuItem.getPreparationTime() != null && menuItem.getPreparationTime() > 0;
        return hasValidName && hasValidPrice && hasValidCategory && hasValidPreparationTime;
    }

    private MenuItemActionResponse buildActionResponse(String type, MenuItem menuItem, String message) {
        return buildActionResponse(type, menuItem, message, null);
    }

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

    private Long resolveCreatedByUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
            if (principal.getUser() != null && principal.getUser().getId() != null) {
                return principal.getUser().getId();
            }
        }

        throw new InvalidOperationException("Authenticated user not found");
    }

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

    private void ensureCurrentUserIsAdminForWorkflow() {
        if (!isCurrentUserAdmin()) {
            throw new InvalidOperationException("Only ADMIN can approve/reject pending menu items");
        }
    }

    private void enforceCurrentUserBranchAccess(Long targetBranchId) {
        Long userBranchId = resolveCurrentUserBranchId();
        if (targetBranchId == null || !userBranchId.equals(targetBranchId)) {
            throw new InvalidOperationException("Menu item access is restricted to your branch");
        }
    }

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