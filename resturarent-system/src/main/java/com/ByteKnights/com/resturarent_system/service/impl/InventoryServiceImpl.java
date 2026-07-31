package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
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
    private final com.ByteKnights.com.resturarent_system.repository.ManagerNotificationRepository notificationRepository;
    private final com.ByteKnights.com.resturarent_system.service.ManagerNotificationService managerNotificationService;

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
     */
    @Override
    public InventorySummaryDTO getInventorySummary(Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);

        List<InventoryItem> items = inventoryItemRepository.findByBranchId(finalBranchId);

        BigDecimal totalValue = BigDecimal.ZERO;
        int lowStockCount = 0;

        for (InventoryItem item : items) {
            if (item.getQuantity() != null && item.getUnitPrice() != null) {
                BigDecimal itemValue = item.getQuantity().multiply(item.getUnitPrice());
                totalValue = totalValue.add(itemValue);
            }

            if (item.getQuantity() != null && item.getReorderLevel() != null) {
                if (item.getQuantity().compareTo(item.getReorderLevel()) <= 0) {
                    lowStockCount++;
                }
            }
        }

        List<ChefRequest> pendingRequests = chefRequestRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(
                finalBranchId,
                ChefRequestStatus.PENDING
        );

        List<ChefRequestDTO> chefRequestDTOs = pendingRequests.stream()
                .map(this::toChefRequestDTO)
                .toList();

        String branchName = branchRepository.findById(finalBranchId)
                .map(Branch::getName)
                .orElse("Unknown Branch");

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
     * This is simple enough for AOP audit.
     * AOP will save the audit row without old/new JSON to reduce storage.
     */
    @Override
    @Auditable(
            module = AuditModule.INVENTORY,
            eventType = AuditEventType.INVENTORY_ITEM_CREATED,
            targetType = AuditTargetType.INVENTORY_ITEM,
            description = "Inventory item created successfully",
            captureResultAsNewValue = false
    )
    @Transactional
    public InventoryItemDTO addInventoryItem(CreateInventoryItemRequest request, Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);

        Branch branch = branchRepository.findById(finalBranchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + finalBranchId));

        InventoryItem item = InventoryItem.builder()
                .name(request.getName())
                .category(request.getCategory())
                .quantity(request.getQuantity())
                .maxStock(request.getQuantity())
                .unit(request.getUnit())
                .reorderLevel(request.getReorderLevel())
                .unitPrice(request.getUnitPrice())
                .branch(branch)
                .build();

        InventoryItem savedItem = inventoryItemRepository.save(item);

        return toItemDTO(savedItem);
    }

    /**
     * 4. Restocks an existing inventory item.
     */
    @Override
    @Transactional
    public InventoryItemDTO restockItem(Long id, RestockInventoryItemRequest request, Long userId) {
        InventoryItem item = getAndVerifyItem(id, userId);

        Map<String, Object> oldValues = buildInventoryItemAuditSnapshot(item);

        BigDecimal previousQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal currentUnitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;

        BigDecimal addedQuantity = request.getQuantity() != null ? request.getQuantity() : BigDecimal.ZERO;
        BigDecimal newBatchUnitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : currentUnitPrice;

        BigDecimal newTotalQuantity = previousQuantity.add(addedQuantity);

        if (newTotalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalValue = previousQuantity.multiply(currentUnitPrice);
            BigDecimal newBatchValue = addedQuantity.multiply(newBatchUnitPrice);
            BigDecimal combinedValue = currentTotalValue.add(newBatchValue);

            BigDecimal weightedAveragePrice = combinedValue.divide(
                    newTotalQuantity,
                    2,
                    java.math.RoundingMode.HALF_UP
            );

            item.setUnitPrice(weightedAveragePrice);
        } else if (request.getUnitPrice() != null) {
            item.setUnitPrice(request.getUnitPrice());
        }

        item.setQuantity(newTotalQuantity);

        InventoryItem savedItem = inventoryItemRepository.save(item);

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
     * 5. Removes stock from an existing inventory item.
     */
    @Override
    @Transactional
    public InventoryItemDTO removeStock(Long id, RemoveInventoryStockRequest request, Long userId) {
        InventoryItem item = getAndVerifyItem(id, userId);

        Map<String, Object> oldValues = buildInventoryItemAuditSnapshot(item);

        BigDecimal previousQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal removedQuantity = request.getQuantity() != null ? request.getQuantity() : BigDecimal.ZERO;
        BigDecimal newQuantity = previousQuantity.subtract(removedQuantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            newQuantity = BigDecimal.ZERO;
        }

        item.setQuantity(newQuantity);

        InventoryItem savedItem = inventoryItemRepository.save(item);

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
     */
    @Override
    @Transactional
    public InventoryItemDTO correctItem(Long id, UpdateInventoryItemRequest request, Long userId) {
        InventoryItem item = getAndVerifyItem(id, userId);

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

        User manager = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        boolean isSuperAdmin = manager.getRole().getName().equals("SUPER_ADMIN");

        if (!isSuperAdmin) {
            Staff staff = staffRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff record not found"));

            if (staff.getBranch() == null || !staff.getBranch().getId().equals(chefRequest.getBranch().getId())) {
                throw new RuntimeException("You do not have permission to resolve requests for this branch.");
            }
        }

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

        // Auto-mark any corresponding notifications as read
        List<com.ByteKnights.com.resturarent_system.entity.ManagerNotification> unreadNotifications = 
            notificationRepository.findByReferenceIdAndTypeAndIsReadFalse(
                updatedRequest.getId(), 
                com.ByteKnights.com.resturarent_system.entity.ManagerNotificationType.CHEF_REQUEST
            );
            
        for (com.ByteKnights.com.resturarent_system.entity.ManagerNotification notif : unreadNotifications) {
            notif.setRead(true);
            notificationRepository.save(notif);
        }
        
        if (!unreadNotifications.isEmpty()) {
            managerNotificationService.pingNotificationResolved(getChefRequestBranchId(updatedRequest));
        }

        return toChefRequestDTO(updatedRequest);
    }

    @Override
    public List<InventoryLogDTO> getInventoryLogs(Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);

        List<InventoryTransactionType> allowedTypes = List.of(
                InventoryTransactionType.RESTOCK,
                InventoryTransactionType.WASTAGE,
                InventoryTransactionType.CORRECTION
        );

        List<InventoryTransaction> transactions = inventoryTransactionRepository
                .findByInventoryItemBranchIdAndTransactionTypeInOrderByCreatedAtDesc(finalBranchId, allowedTypes);

        return transactions.stream()
                .map(this::toLogDTO)
                .collect(Collectors.toList());
    }

    private InventoryLogDTO toLogDTO(InventoryTransaction transaction) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

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

        String notes = transaction.getNotes();
        if (notes == null || notes.trim().isEmpty()) {
            notes = "No notes provided.";
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
                .notes(notes)
                .build();
    }

    private InventoryItem getAndVerifyItem(Long id, Long userId) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + id));

        Long userBranchId = resolveBranchId(null, userId);

        if (!item.getBranch().getId().equals(userBranchId)) {
            throw new IllegalArgumentException("Unauthorized: Item does not belong to your branch.");
        }

        return item;
    }

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

    private InventoryItemDTO toItemDTO(InventoryItem item) {
        String derivedStatus = "good";

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

    private ChefRequestDTO toChefRequestDTO(ChefRequest req) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String formattedTime = req.getCreatedAt() != null
                ? req.getCreatedAt().format(timeFormatter)
                : "";

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

    private String generateAvatarColor(String name) {
        String[] colors = { "#F97316", "#3B82F6", "#10B981", "#8B5CF6", "#EF4444", "#EC4899" };

        if (name == null || name.isEmpty()) {
            return colors[0];
        }

        int hash = Math.abs(name.hashCode());
        return colors[hash % colors.length];
    }

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

    private Long getInventoryItemBranchId(InventoryItem item) {
        if (item == null || item.getBranch() == null) {
            return null;
        }

        return item.getBranch().getId();
    }

    private Long getChefRequestBranchId(ChefRequest request) {
        if (request == null || request.getBranch() == null) {
            return null;
        }

        return request.getBranch().getId();
    }
}