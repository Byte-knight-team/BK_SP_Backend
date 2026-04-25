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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    private static final BigDecimal MAX_MENU_ITEM_PRICE = new BigDecimal("99999999.99");
    private static final int MAX_PREPARATION_TIME_MINUTES = 240;

    private final MenuItemRepository menuItemRepository;
    private final BranchRepository branchRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final UserRepository userRepository;

    public MenuServiceImpl(MenuItemRepository menuItemRepository, BranchRepository branchRepository, MenuCategoryRepository menuCategoryRepository, UserRepository userRepository) {
        this.menuItemRepository = menuItemRepository;
        this.branchRepository = branchRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.userRepository = userRepository;
    }

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

        Long creatorUserId = resolveCreatedByUserId();
        boolean creatorIsAdmin = isCurrentUserAdminOrSuperAdmin();

        MenuItem menuItem = MenuItem.builder()
                .branch(branch)
                .category(category)
            .subCategory(validateAndNormalizeOptionalSubCategory(request.getSubCategory()))
                .name(validatedName)
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(validateAndNormalizeImageUrl(request.getImageUrl()))
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : false)
                .status(MenuItemStatus.PENDING)
                .preparationTime(request.getPreparationTime())
                .createdBy(creatorUserId)
                .build();

        boolean hasAllRequiredApprovalFields = isMenuItemValidForApproval(menuItem);

        if (creatorIsAdmin) {
            // Admin-created items are immediately ACTIVE only when complete.
            // If required fields are missing, keep them as DRAFT for later completion.
            if (hasAllRequiredApprovalFields) {
                menuItem.setStatus(MenuItemStatus.ACTIVE);
                menuItem.setIsAvailable(true);
                menuItem.setApprovedAt(LocalDateTime.now());
                menuItem.setRejectedAt(null);
                menuItem.setRejectionReason(null);
            } else {
                menuItem.setStatus(MenuItemStatus.DRAFT);
                menuItem.setIsAvailable(false);
                menuItem.setApprovedAt(null);
                menuItem.setRejectedAt(null);
                menuItem.setRejectionReason(null);
            }
        } else if (!hasAllRequiredApprovalFields) {
            // Chef-created incomplete items stay in DRAFT until completed and submitted.
            menuItem.setStatus(MenuItemStatus.DRAFT);
            menuItem.setIsAvailable(false);
        } else {
            // Chef-created valid items enter approval queue.
            menuItem.setStatus(MenuItemStatus.PENDING);
            menuItem.setIsAvailable(false);
        }

        MenuItem saved = menuItemRepository.save(menuItem);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findByStatusAndIsAvailableTrue(MenuItemStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getPendingChefMenuItems() {
        List<Long> chefUserIds = userRepository.findUserIdsByRoleKeyword("CHEF");
        if (chefUserIds.isEmpty()) {
            return List.of();
        }

        return menuItemRepository.findByStatusAndCreatedByIn(MenuItemStatus.PENDING, chefUserIds)
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
        return menuItemRepository.findDistinctSubCategoriesByBranchIdAndCategoryId(branchId, categoryId);
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

        return buildActionResponse("ACTIVE", menuItem, "Item approved and activated");
    }

    @Override
    @Transactional
    public MenuItemActionResponse rejectMenuItem(Long id, RejectMenuItemRequest request) {
        ensureCurrentUserIsAdminForWorkflow();
        MenuItem menuItem = findMenuItemOrThrow(id);

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

        return buildActionResponse("REJECTED", menuItem, "Item rejected: " + rejectionReason.trim());
    }

    @Override
    @Transactional
    public MenuItemActionResponse toggleMenuItemAvailability(Long id, boolean isAvailable) {
        MenuItem menuItem = findMenuItemOrThrow(id);

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

        return normalizedSubCategory;
    }

    private boolean isMenuItemValidForApproval(MenuItem menuItem) {
        boolean hasValidName = menuItem.getName() != null && !menuItem.getName().isBlank();
        boolean hasValidPrice = menuItem.getPrice() != null && menuItem.getPrice().compareTo(BigDecimal.ZERO) > 0;
        boolean hasValidCategory = menuItem.getCategory() != null;
        boolean hasValidPreparationTime = menuItem.getPreparationTime() != null && menuItem.getPreparationTime() > 0;
        return hasValidName && hasValidPrice && hasValidCategory && hasValidPreparationTime;
    }

    private MenuItemActionResponse buildActionResponse(String type, MenuItem menuItem, String message) {
        return MenuItemActionResponse.builder()
                .type(type)
                .menuItemId(menuItem.getId())
                .menuItemName(menuItem.getName())
                .message(message)
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

        List<Long> chefUserIds = userRepository.findUserIdsByRoleKeyword("CHEF");
        if (!chefUserIds.isEmpty()) {
            return chefUserIds.get(0);
        }
        return 1L;
    }

    private boolean isCurrentUserAdminOrSuperAdmin() {
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
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority) || "ROLE_SUPER_ADMIN".equals(authority));
    }

    private void ensureCurrentUserIsAdminForWorkflow() {
        if (!isCurrentUserAdminOrSuperAdmin()) {
            throw new InvalidOperationException("Only ADMIN or SUPER_ADMIN can approve/reject pending menu items");
        }
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