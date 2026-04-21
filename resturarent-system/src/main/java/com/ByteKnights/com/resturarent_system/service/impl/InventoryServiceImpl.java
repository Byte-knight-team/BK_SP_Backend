package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.InventoryItemDTO;
import com.ByteKnights.com.resturarent_system.entity.ChefRequest;
import com.ByteKnights.com.resturarent_system.entity.InventoryItem;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.ChefRequestRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.service.InventoryService;

import lombok.RequiredArgsConstructor;

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

    /**
     * Implementation of getAllItemsByBranch.
     * 
     * 1. Fetches all raw InventoryItem entities from the database that belong to
     * the branch.
     * 2. Uses the Java Stream API to pass each entity through our private
     * `toItemDTO` mapper.
     * 3. Collects the mapped DTOs into a List and returns it.
     */
    @Override
    public List<InventoryItemDTO> getAllItemsByBranch(Long branchId) {

        List<InventoryItem> items = inventoryItemRepository.findByBranchId(branchId);

        return items.stream()
                .map(item -> toItemDTO(item))
                .collect(Collectors.toList());
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
                .role(req.getChefRole())
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
