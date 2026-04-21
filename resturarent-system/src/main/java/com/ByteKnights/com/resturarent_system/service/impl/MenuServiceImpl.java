package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.customer.MenuItemResponse;
import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemStatus;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuItemRepository menuItemRepository;

    public MenuServiceImpl(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public List<MenuItemResponse> fetchCustomerMenu(Long branchId) {
        // ENFORCE BUSINESS RULE: Default to Branch 1 for Online customers
        // only branch 1 is doing online services
        Long targetBranchId = (branchId != null) ? branchId : 1L;

        // Fetch only APPROVED and AVAILABLE items for this specific branch
        List<MenuItem> items = menuItemRepository.findByBranchIdAndStatusAndIsAvailableTrue(
                targetBranchId, 
                MenuItemStatus.APPROVED
        );

        // Convert the database Entities into clean DTOs for React
        return items.stream().map(item -> MenuItemResponse.builder()
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
}