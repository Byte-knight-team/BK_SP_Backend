package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.inventory.CreateInventoryItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.inventory.RemoveInventoryStockRequest;
import com.ByteKnights.com.resturarent_system.dto.request.inventory.RestockInventoryItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.inventory.UpdateInventoryItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.inventory.ResolveChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryLogDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventorySummaryDTO;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.ChefRequest;
import com.ByteKnights.com.resturarent_system.entity.InventoryItem;
import com.ByteKnights.com.resturarent_system.entity.InventoryTransaction;
import com.ByteKnights.com.resturarent_system.entity.InventoryTransactionType;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.entity.ChefRequestStatus;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.ChefRequestRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryTransactionRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The actual implementation of the InventoryService.
 *
 * The @Service annotation tells Spring Boot that this class holds business logic.
 * Spring will automatically register this as a "Bean" and inject it wherever
 * InventoryService is requested, like in your Controller.
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    /**
     * INJECTED REPOSITORIES
     *
     * Because of the @RequiredArgsConstructor annotation on the class,
     * Lombok generates a constructor that requires these repositories.
     * Spring Boot then automatically injects the live database connections into
     * these variables at runtime.
     */
    private final InventoryItemRepository inventoryItemRepository;
    private final ChefRequestRepository chefRequestRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * Helper method to securely resolve the branch ID.
     */
    private Long resolveBranchId(Long targetBranchId, Long userId) {
        if (targetBranchId != null) {
            return targetBranchId; // Super Admin provided a specific branch
        }

        // Everyone else: Look up their assigned branch from the database
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not assigned to any branch as staff"));

        return staff.getBranch().getId();
    }

    /**
     * 1. Implementation of getAllItemsByBranch.
     *
     * 1. Fetches all raw InventoryItem entities from the database that belong to
     * the branch.
     * 2. Uses the Java Stream API to pass each entity through our private
     * toItemDTO mapper.
     * 3. Collects the mapped DTOs into a List and returns it.
     */
    @Override
    public List<InventoryItemDTO> getAllItemsByBranch(Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);

        List<InventoryItem> items = inventoryItemRepository.findByBranchId(finalBranchId);

        return items.stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    /**
     * 2. Implements the logic to calculate the inventory dashboard summary.
     *
     * @param targetBranchId The ID of the branch, if Super Admin.
     * @param userId         The ID of the logged in user.
     * @return InventorySummaryDTO populated with real-time calculated metrics.
     */
    @Override
    public InventorySummaryDTO getInventorySummary(Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);

        // 1. Fetch all items from the database for this specific branch
        List<InventoryItem> items = inventoryItemRepository.findByBranchId(finalBranchId);

        BigDecimal totalValue = BigDecimal.ZERO;
        int lowStockCount = 0;

        /*
         * 2. Loop through every single inventory item to calculate two things:
         * a) The total financial value of the entire inventory.
         * b) How many items have critically low stock.
         */
        for (InventoryItem item : items) {
            // Calculate Total Value: Item Quantity * Item Unit Price
            if (item.getQuantity() != null && item.getUnitPrice() != null) {
                BigDecimal itemValue = item.getQuantity().multiply(item.getUnitPrice());
                totalValue = totalValue.add(itemValue);
            }

            // Calculate Low Stock: If current stock <= reorder threshold, trigger an alert
            if (item.getQuantity() != null && item.getReorderLevel() != null) {
                if (item.getQuantity().compareTo(item.getReorderLevel()) <= 0) {
                    lowStockCount++;
                }
            }
        }

        /*
         * 3. Fetch all Chef Requests for this branch that are currently PENDING.
         * We do not show APPROVED or REJECTED requests on the dashboard.
         */
        List<ChefRequest> pendingRequests = chefRequestRepository.findByBranchIdAndStatus(
                finalBranchId,
                ChefRequestStatus.PENDING
        );

        // Convert the raw database entities into clean DTOs for the frontend
        List<ChefRequestDTO> chefRequestDTOs = pendingRequests.stream()
                .map(this::toChefRequestDTO)
                .toList();

        // 4. Fetch Branch name for the header
        String branchName = branchRepository.findById(finalBranchId)
                .map(Branch::getName)
                .orElse("Unknown Branch");

        /*
         * 5. Package all our calculations into the Summary DTO and return it.
         */
        return InventorySummaryDTO.builder()
                .branch(branchName)
                .totalInventoryValue(totalValue)
                .lowStockAlerts(lowStockCount)
                .pendingChefDrafts(chefRequestDTOs.size())
                .chefRequests(chefRequestDTOs)
                .build();
    }

    /**
     * 3. Adds a new inventory item to the database.
     *
     * This method validates the existence of the branch, maps the request
     * data to a new InventoryItem entity, and saves it. It then returns
     * the saved item as a DTO.
     */
    @Override
    @Transactional
    public InventoryItemDTO addInventoryItem(CreateInventoryItemRequest request, Long targetBranchId, Long userId) {
        // 1. Resolve the actual branch ID, staff lookup if targetBranchId is null
        Long finalBranchId = resolveBranchId(targetBranchId, userId);

        // 2. Verify the branch exists
        Branch branch = branchRepository.findById(finalBranchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + finalBranchId));

        // 3. Build the entity from the request data
        InventoryItem item = InventoryItem.builder()
                .name(request.getName())
                .category(request.getCategory())
                .quantity(request.getQuantity())
                .maxStock(request.getQuantity()) // Set initial stock as max stock
                .unit(request.getUnit())
                .reorderLevel(request.getReorderLevel())
                .unitPrice(request.getUnitPrice())
                .branch(branch)
                .build();

        // 4. Save the new item to the database
        InventoryItem savedItem = inventoryItemRepository.save(item);

        /*
         * Manual audit is used because we want clean newValuesJson for the created
         * inventory item.
         */
        auditLogService.logCurrentUserAction(
                AuditModule.INVENTORY,
                AuditEventType.INVENTORY_ITEM_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.INVENTORY_ITEM,
                savedItem.getId(),
                getInventoryItemBranchId(savedItem),
                "Inventory item created successfully",
                null,
                buildInventoryItemAuditSnapshot(savedItem)
        );

        // 5. Return the DTO
        return toItemDTO(savedItem);
    }

    /**
     * 4. Restocks an existing inventory item.
     *
     * Logic:
     * 1. Fetch item and verify branch access.
     * 2. Increment stock quantity.
     * 3. Update unit price if provided.
     * 4. Save and return updated DTO.
     */
    @Override
    @Transactional
    public InventoryItemDTO restockItem(Long id, RestockInventoryItemRequest request, Long userId) {
        InventoryItem item = getAndVerifyItem(id, userId);

        /*
         * Capture old values before stock quantity and unit price change.
         */
        Map<String, Object> oldValues = buildInventoryItemAuditSnapshot(item);

        BigDecimal previousQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal currentUnitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;

        BigDecimal addedQuantity = request.getQuantity() != null ? request.getQuantity() : BigDecimal.ZERO;
        BigDecimal newBatchUnitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : currentUnitPrice;

        // 1. Calculate the New Total Quantity
        BigDecimal newTotalQuantity = previousQuantity.add(addedQuantity);

        // 2. Calculate the Weighted Average Cost (WAC)
        // Formula: ((Old Qty * Old Price) + (New Qty * New Price)) / Total Qty
        if (newTotalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalValue = previousQuantity.multiply(currentUnitPrice);
            BigDecimal newBatchValue = addedQuantity.multiply(newBatchUnitPrice);
            BigDecimal combinedValue = currentTotalValue.add(newBatchValue);

            // Use 2 decimal places for final unit price
            BigDecimal weightedAveragePrice = combinedValue.divide(
                    newTotalQuantity,
                    2,
                    java.math.RoundingMode.HALF_UP
            );

            item.setUnitPrice(weightedAveragePrice);
        } else if (request.getUnitPrice() != null) {
            // If total quantity is still zero, just use the provided price
            item.setUnitPrice(request.getUnitPrice());
        }

        item.setQuantity(newTotalQuantity);

        InventoryItem savedItem = inventoryItemRepository.save(item);

        // Log inventory transaction in transaction table
        InventoryTransaction transaction = saveTransaction(
                savedItem,
                userId,
                InventoryTransactionType.RESTOCK,
                addedQuantity,
                previousQuantity,
                savedItem.getQuantity(),
                newBatchUnitPrice,
                request.getNotes()
        );

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("inventoryItem", buildInventoryItemAuditSnapshot(savedItem));
        newValues.put("transaction", buildInventoryTransactionAuditSnapshot(transaction));

        auditLogService.logCurrentUserAction(
                AuditModule.INVENTORY,
                AuditEventType.INVENTORY_ITEM_RESTOCKED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.INVENTORY_ITEM,
                savedItem.getId(),
                getInventoryItemBranchId(savedItem),
                "Inventory item restocked successfully",
                oldValues,
                newValues
        );

        return toItemDTO(savedItem);
    }

    /**
     * 5. Removes stock from an existing inventory item, for wastage/damage.
     *
     * Logic:
     * 1. Fetch item and verify branch access.
     * 2. Decrement stock quantity.
     * 3. Ensure stock does not go below zero.
     * 4. Save and return updated DTO.
     */
    @Override
    @Transactional
    public InventoryItemDTO removeStock(Long id, RemoveInventoryStockRequest request, Long userId) {
        InventoryItem item = getAndVerifyItem(id, userId);

        /*
         * Capture old values before stock is removed.
         */
        Map<String, Object> oldValues = buildInventoryItemAuditSnapshot(item);

        BigDecimal previousQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal removedQuantity = request.getQuantity() != null ? request.getQuantity() : BigDecimal.ZERO;
        BigDecimal newQuantity = previousQuantity.subtract(removedQuantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            newQuantity = BigDecimal.ZERO;
        }

        item.setQuantity(newQuantity);

        InventoryItem savedItem = inventoryItemRepository.save(item);

        // Log inventory transaction in transaction table
        InventoryTransaction transaction = saveTransaction(
                savedItem,
                userId,
                InventoryTransactionType.WASTAGE,
                removedQuantity.negate(),
                previousQuantity,
                savedItem.getQuantity(),
                savedItem.getUnitPrice(),
                request.getReason()
        );

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("inventoryItem", buildInventoryItemAuditSnapshot(savedItem));
        newValues.put("transaction", buildInventoryTransactionAuditSnapshot(transaction));

        auditLogService.logCurrentUserAction(
                AuditModule.INVENTORY,
                AuditEventType.INVENTORY_STOCK_REMOVED,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.INVENTORY_ITEM,
                savedItem.getId(),
                getInventoryItemBranchId(savedItem),
                "Inventory stock removed successfully",
                oldValues,
                newValues
        );

        return toItemDTO(savedItem);
    }

    /**
     * 6. Corrects or updates an existing inventory item's details.
     *
     * Logic:
     * 1. Fetch item and verify branch access.
     * 2. Overwrite all fields with the correction data.
     * 3. Save and return updated DTO.
     */
    @Override
    @Transactional
    public InventoryItemDTO correctItem(Long id, UpdateInventoryItemRequest request, Long userId) {
        InventoryItem item = getAndVerifyItem(id, userId);

        /*
         * Capture old values before item correction.
         */
        Map<String, Object> oldValues = buildInventoryItemAuditSnapshot(item);

        BigDecimal previousQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;

        item.setName(request.getName());
        item.setCategory(request.getCategory());
        item.setUnit(request.getUnit());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());
        item.setReorderLevel(request.getReorderLevel());

        InventoryItem savedItem = inventoryItemRepository.save(item);

        BigDecimal savedQuantity = savedItem.getQuantity() != null ? savedItem.getQuantity() : BigDecimal.ZERO;

        // Log inventory transaction in transaction table
        InventoryTransaction transaction = saveTransaction(
                savedItem,
                userId,
                InventoryTransactionType.CORRECTION,
                savedQuantity.subtract(previousQuantity),
                previousQuantity,
                savedQuantity,
                savedItem.getUnitPrice(),
                request.getNotes()
        );

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("inventoryItem", buildInventoryItemAuditSnapshot(savedItem));
        newValues.put("transaction", buildInventoryTransactionAuditSnapshot(transaction));

        auditLogService.logCurrentUserAction(
                AuditModule.INVENTORY,
                AuditEventType.INVENTORY_ITEM_CORRECTED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.INVENTORY_ITEM,
                savedItem.getId(),
                getInventoryItemBranchId(savedItem),
                "Inventory item corrected successfully",
                oldValues,
                newValues
        );

        return toItemDTO(savedItem);
    }

    /**
     * 7. Resolves a chef request by approving or rejecting it.
     */
    @Override
    @Transactional
    public ChefRequestDTO resolveChefRequest(Long requestId, ResolveChefRequestDTO requestDTO, Long userId) {
        ChefRequest chefRequest = chefRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef request not found with ID: " + requestId));

        // Get the manager user resolving the request
        User manager = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        // Validate branch access if the manager is not SUPER_ADMIN
        boolean isSuperAdmin = manager.getRole().getName().equals("SUPER_ADMIN");

        if (!isSuperAdmin) {
            Staff staff = staffRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff record not found"));

            if (staff.getBranch() == null || !staff.getBranch().getId().equals(chefRequest.getBranch().getId())) {
                throw new RuntimeException("You do not have permission to resolve requests for this branch.");
            }
        }

        /*
         * Capture old chef request state before status/manager note update.
         */
        Map<String, Object> oldValues = buildChefRequestAuditSnapshot(chefRequest);

        ChefRequestStatus newStatus;
        String statusStr = requestDTO.getStatus().toUpperCase();

        if ("ACCEPTED".equals(statusStr) || "APPROVED".equals(statusStr)) {
            newStatus = ChefRequestStatus.APPROVED;
        } else if ("REJECTED".equals(statusStr)) {
            newStatus = ChefRequestStatus.REJECTED;
        } else {
            try {
                newStatus = ChefRequestStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + requestDTO.getStatus());
            }
        }

        chefRequest.setStatus(newStatus);
        chefRequest.setManagerNote(requestDTO.getManagerNote());

        ChefRequest updatedRequest = chefRequestRepository.saveAndFlush(chefRequest);

        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.CHEF_REQUEST_RESOLVED,
                AuditStatus.SUCCESS,
                newStatus == ChefRequestStatus.REJECTED ? AuditSeverity.WARN : AuditSeverity.INFO,
                AuditTargetType.CHEF_REQUEST,
                updatedRequest.getId(),
                getChefRequestBranchId(updatedRequest),
                "Chef inventory request resolved successfully",
                oldValues,
                buildChefRequestAuditSnapshot(updatedRequest)
        );

        return toChefRequestDTO(updatedRequest);
    }

    @Override
    public List<InventoryLogDTO> getInventoryLogs(Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);

        // Fetch all transactions for this branch, ordered by newest first
        List<InventoryTransaction> transactions = inventoryTransactionRepository
                .findByInventoryItemBranchIdOrderByCreatedAtDesc(finalBranchId);

        return transactions.stream()
                .map(this::toLogDTO)
                .collect(Collectors.toList());
    }

    /**
     * Private helper to convert an InventoryTransaction entity to a DTO.
     * Populates all detail fields for the Log Detail popup modal.
     */
    private InventoryLogDTO toLogDTO(InventoryTransaction transaction) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

        // Resolve the staff member's display name
        String performedBy = "Unknown";

        if (transaction.getStaff() != null) {
            String first = transaction.getStaff().getFirstName();
            String last = transaction.getStaff().getLastName();

            if (first != null && last != null) {
                performedBy = first + " " + last;
            } else if (transaction.getStaff().getUser() != null
                    && transaction.getStaff().getUser().getFullName() != null) {
                performedBy = transaction.getStaff().getUser().getFullName();
            }
        }

        // Resolve item metadata
        String category = "";
        String unit = "";

        if (transaction.getInventoryItem() != null) {
            category = transaction.getInventoryItem().getCategory() != null
                    ? transaction.getInventoryItem().getCategory()
                    : "";

            unit = transaction.getInventoryItem().getUnit() != null
                    ? transaction.getInventoryItem().getUnit()
                    : "";
        }

        return InventoryLogDTO.builder()
                .id(transaction.getId())
                .itemName(transaction.getInventoryItem().getName())
                .category(category)
                .unit(unit)
                .updatedAt(transaction.getCreatedAt().format(formatter))
                .updateType(transaction.getTransactionType().name())
                .quantityChange(transaction.getQuantityChange())
                .previousQuantity(transaction.getPreviousQuantity())
                .newQuantity(transaction.getNewQuantity())
                .unitPrice(transaction.getUnitPrice())
                .performedBy(performedBy)
                .notes(transaction.getNotes())
                .build();
    }

    /**
     * Internal helper to fetch an item and ensure the user has permission to update it.
     */
    private InventoryItem getAndVerifyItem(Long id, Long userId) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + id));

        Long userBranchId = resolveBranchId(null, userId);

        if (!item.getBranch().getId().equals(userBranchId)) {
            throw new IllegalArgumentException("Unauthorized: Item does not belong to your branch.");
        }

        return item;
    }

    /**
     * Private helper to log a stock transaction in the inventory transaction table.
     *
     * This returns the saved transaction so audit_logs can include the transaction ID
     * and quantity movement details in newValuesJson.
     */
    private InventoryTransaction saveTransaction(
            InventoryItem item,
            Long userId,
            InventoryTransactionType type,
            BigDecimal change,
            BigDecimal prevQty,
            BigDecimal newQty,
            BigDecimal price,
            String notes
    ) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found for user id: " + userId));

        InventoryTransaction transaction = InventoryTransaction.builder()
                .inventoryItem(item)
                .staff(staff)
                .transactionType(type)
                .quantityChange(change)
                .previousQuantity(prevQty)
                .newQuantity(newQty)
                .unitPrice(price)
                .notes(notes)
                .build();

        return inventoryTransactionRepository.save(transaction);
    }

    // ───────────────────────── PRIVATE HELPER MAPPERS ─────────────────────────

    /**
     * Converts an InventoryItem entity to a DTO for the frontend.
     * Calculates if the item is in a warning state, stock <= reorder level.
     */
    private InventoryItemDTO toItemDTO(InventoryItem item) {
        String derivedStatus = "good";

        // If the current stock level is less than or equal to the reorder threshold, it is a warning
        if (item.getQuantity() != null && item.getReorderLevel() != null) {
            if (item.getQuantity().compareTo(item.getReorderLevel()) <= 0) {
                derivedStatus = "warning";
            }
        }

        return InventoryItemDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .unitPrice(item.getUnitPrice())
                .unit(item.getUnit())
                .stockLevel(item.getQuantity())
                .status(derivedStatus)
                .build();
    }

    /**
     * Converts a ChefRequest entity to a DTO for the frontend.
     * Formats the timestamp and generates an avatar color based on the chef's name.
     */
    private ChefRequestDTO toChefRequestDTO(ChefRequest req) {
        // Format time to simple HH:mm format, e.g. 14:20
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String formattedTime = req.getCreatedAt() != null
                ? req.getCreatedAt().format(timeFormatter)
                : "";

        // Format quantity string, e.g. 20.0 kg
        String formattedQuantity = req.getRequestedQuantity() + " " + req.getUnit();

        return ChefRequestDTO.builder()
                .id(req.getId())
                .chefName(req.getChefName())
                .time(formattedTime)
                .item(req.getItemName())
                .quantity(formattedQuantity)
                .note(req.getChefNote())
                .managerNote(req.getManagerNote())
                .status(req.getStatus() != null ? req.getStatus().name() : "PENDING")
                .requestType(req.getRequestType() != null ? req.getRequestType().name() : null)
                .avatarColor(generateAvatarColor(req.getChefName()))
                .build();
    }

    /**
     * Generates a consistent hex color based on the chef's name string hash.
     * This avoids having to store color preferences in the database.
     */
    private String generateAvatarColor(String name) {
        String[] colors = { "#F97316", "#3B82F6", "#10B981", "#8B5CF6", "#EF4444", "#EC4899" };

        if (name == null || name.isEmpty()) {
            return colors[0];
        }

        int hash = Math.abs(name.hashCode());
        return colors[hash % colors.length];
    }

    /*
     * Builds safe audit JSON for inventory item create/restock/remove/correct actions.
     */
    private Map<String, Object> buildInventoryItemAuditSnapshot(InventoryItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (item == null) {
            return snapshot;
        }

        snapshot.put("inventoryItemId", item.getId());
        snapshot.put("name", item.getName());
        snapshot.put("category", item.getCategory());
        snapshot.put("quantity", item.getQuantity());
        snapshot.put("maxStock", item.getMaxStock());
        snapshot.put("unit", item.getUnit());
        snapshot.put("reorderLevel", item.getReorderLevel());
        snapshot.put("unitPrice", item.getUnitPrice());

        snapshot.put("branchId", item.getBranch() != null ? item.getBranch().getId() : null);
        snapshot.put("branchName", item.getBranch() != null ? item.getBranch().getName() : null);

        return snapshot;
    }

    /*
     * Builds safe audit JSON for inventory transaction details.
     */
    private Map<String, Object> buildInventoryTransactionAuditSnapshot(InventoryTransaction transaction) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (transaction == null) {
            return snapshot;
        }

        Staff staff = transaction.getStaff();

        snapshot.put("transactionId", transaction.getId());
        snapshot.put("inventoryItemId",
                transaction.getInventoryItem() != null ? transaction.getInventoryItem().getId() : null);
        snapshot.put("inventoryItemName",
                transaction.getInventoryItem() != null ? transaction.getInventoryItem().getName() : null);

        snapshot.put("transactionType",
                transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null);
        snapshot.put("quantityChange", transaction.getQuantityChange());
        snapshot.put("previousQuantity", transaction.getPreviousQuantity());
        snapshot.put("newQuantity", transaction.getNewQuantity());
        snapshot.put("unitPrice", transaction.getUnitPrice());
        snapshot.put("notes", transaction.getNotes());
        snapshot.put("createdAt", transaction.getCreatedAt());

        snapshot.put("staffId", staff != null ? staff.getId() : null);
        snapshot.put("staffUserId",
                staff != null && staff.getUser() != null ? staff.getUser().getId() : null);
        snapshot.put("staffName",
                staff != null && staff.getUser() != null ? staff.getUser().getFullName() : null);

        return snapshot;
    }

    /*
     * Builds safe audit JSON for chef request resolution.
     */
    private Map<String, Object> buildChefRequestAuditSnapshot(ChefRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (request == null) {
            return snapshot;
        }

        snapshot.put("chefRequestId", request.getId());
        snapshot.put("branchId", request.getBranch() != null ? request.getBranch().getId() : null);
        snapshot.put("branchName", request.getBranch() != null ? request.getBranch().getName() : null);
        snapshot.put("chefName", request.getChefName());
        snapshot.put("itemName", request.getItemName());
        snapshot.put("requestedQuantity", request.getRequestedQuantity());
        snapshot.put("unit", request.getUnit());
        snapshot.put("chefNote", request.getChefNote());
        snapshot.put("managerNote", request.getManagerNote());
        snapshot.put("status", request.getStatus() != null ? request.getStatus().name() : null);
        snapshot.put("requestType", request.getRequestType() != null ? request.getRequestType().name() : null);
        snapshot.put("createdAt", request.getCreatedAt());

        return snapshot;
    }

    /*
     * Gets inventory item branch ID for audit branch filtering.
     */
    private Long getInventoryItemBranchId(InventoryItem item) {
        if (item == null || item.getBranch() == null) {
            return null;
        }

        return item.getBranch().getId();
    }

    /*
     * Gets chef request branch ID for audit branch filtering.
     */
    private Long getChefRequestBranchId(ChefRequest request) {
        if (request == null || request.getBranch() == null) {
            return null;
        }

        return request.getBranch().getId();
    }
}