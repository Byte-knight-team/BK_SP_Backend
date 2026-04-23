package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.inventory.CreateInventoryItemRequest;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventorySummaryDTO;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.ChefRequest;
import com.ByteKnights.com.resturarent_system.entity.InventoryItem;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.ChefRequestRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.InventoryService;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * The actual implementation of the InventoryService.
 * 
 * The @Service annotation tells Spring Boot that this class holds business
 * logic.
 * Spring will automatically register this as a "Bean" and inject it wherever
 * InventoryService is requested (like in your Controller).
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    /**
     * INJECTED REPOSITORIES
     * 
     * Because of the @RequiredArgsConstructor annotation on the class,
     * Lombok generates a constructor that requires these three repositories.
     * Spring Boot then automatically "injects" the live database connections into
     * these variables at runtime.
     */
    private final InventoryItemRepository inventoryItemRepository;
    private final ChefRequestRepository chefRequestRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository; // Added StaffRepository

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
     * 1.______Implementation of getAllItemsByBranch.
     * 
     * 1. Fetches all raw InventoryItem entities from the database that belong to
     * the branch.
     * 2. Uses the Java Stream API to pass each entity through our private
     * `toItemDTO` mapper.
     * 3. Collects the mapped DTOs into a List and returns it.
     */
    @Override
    public List<InventoryItemDTO> getAllItemsByBranch(Long targetBranchId, Long userId) {

        Long finalBranchId = resolveBranchId(targetBranchId, userId);

        List<InventoryItem> items = inventoryItemRepository.findByBranchId(finalBranchId);

        return items.stream()
                .map(item -> toItemDTO(item))
                .collect(Collectors.toList());
    }

    /**
     * 2.______________ Implements the logic to calculate the inventory dashboard
     * summary.
     * 
     * @param targetBranchId The ID of the branch (if Super Admin).
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

            // Calculate Total Value: (Item Quantity * Item Unit Price)
            if (item.getQuantity() != null && item.getUnitPrice() != null) {
                BigDecimal itemValue = item.getQuantity().multiply(item.getUnitPrice());
                totalValue = totalValue.add(itemValue);
            }

            // Calculate Low Stock: If current stock <= reorder threshold, trigger an alert!
            if (item.getQuantity() != null && item.getReorderLevel() != null) {
                if (item.getQuantity().compareTo(item.getReorderLevel()) <= 0) {
                    lowStockCount++; // Increment the alert counter
                }
            }
        }

        /*
         * 3. Fetch all Chef Requests for this branch that are currently 'PENDING'.
         * We don't want to show APPROVED or REJECTED requests on the dashboard.
         */
        List<ChefRequest> pendingRequests = chefRequestRepository.findByBranchIdAndStatus(
                finalBranchId,
                com.ByteKnights.com.resturarent_system.entity.ChefRequestStatus.PENDING);

        // Convert the raw database entities into clean DTOs for the frontend
        List<ChefRequestDTO> chefRequestDTOs = pendingRequests.stream()
                .map(this::toChefRequestDTO)
                .toList();

        /*
         * 4. Package all our calculations into the Summary DTO and return it.
         */
        return InventorySummaryDTO.builder()
                .branch("Colombo Main") // Hardcoded for now; can fetch from Branch entity later
                .totalInventoryValue(totalValue)
                .lowStockAlerts(lowStockCount)
                .pendingChefDrafts(chefRequestDTOs.size()) // The number of pending drafts is just the size of the list!
                .chefRequests(chefRequestDTOs)
                .build();
    }

    /**
     * 3.__________ Adds a new inventory item to the database.
     * 
     * This method validates the existence of the branch, maps the request
     * data to a new InventoryItem entity, and saves it. It then returns
     * the saved item as a DTO.
     * 
     * @param request        The DTO containing item details (name, qty, price,
     *                       etc).
     * @param targetBranchId The ID of the branch the item belongs to.
     * @return The newly created InventoryItemDTO.
     */
    @Override
    @Transactional
    public InventoryItemDTO addInventoryItem(CreateInventoryItemRequest request, Long targetBranchId, Long userId) {
        // 1. Resolve the actual branch ID (staff lookup if targetBranchId is null)
        Long finalBranchId = resolveBranchId(targetBranchId, userId);

        // 1. Verify the branch exists
        Branch branch = branchRepository.findById(finalBranchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + finalBranchId));

        // 2. Build the entity from the request data
        InventoryItem item = InventoryItem.builder()
                .name(request.getName())
                .category(request.getCategory())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .reorderLevel(request.getReorderLevel())
                .unitPrice(request.getUnitPrice())
                .branch(branch)
                .build();

        // 3. Save the new item to the database
        InventoryItem savedItem = inventoryItemRepository.save(item);

        // 4. Return the DTO
        return toItemDTO(savedItem);
    }

    // ───────────────────────── PRIVATE HELPER MAPPERS ─────────────────────────

    /**
     * Converts an InventoryItem entity to a DTO for the frontend.
     * Calculates if the item is in a "warning" state (stock <= reorder level).
     */
    private InventoryItemDTO toItemDTO(InventoryItem item) {
        String derivedStatus = "good";

        // If the current stock level is less than or equal to the reorder threshold,
        // it's a warning
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
        // Format time to simple "HH:mm" format (e.g. 14:20)
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = req.getCreatedAt() != null
                ? req.getCreatedAt().format(timeFormatter)
                : "";

        // Format quantity string (e.g., "20.0 kg")
        String formattedQuantity = req.getRequestedQuantity() + " " + req.getUnit();

        return ChefRequestDTO.builder()
                .id(req.getId())
                .chefName(req.getChefName())
                .time(formattedTime)
                .item(req.getItemName())
                .quantity(formattedQuantity)
                .note(req.getChefNote())
                .status(req.getStatus() != null ? req.getStatus().name() : "PENDING")
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

}
