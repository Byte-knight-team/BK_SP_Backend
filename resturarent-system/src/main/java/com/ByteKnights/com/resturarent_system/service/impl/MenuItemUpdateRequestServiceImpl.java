package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.admin.MenuItemUpdateDecisionDto;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemUpdateRequestDto;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemUpdateRequestResponseDto;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemUpdateRequestRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.MenuItemUpdateRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemUpdateRequestServiceImpl implements MenuItemUpdateRequestService {

    private final MenuItemUpdateRequestRepository repository;
    private final StaffRepository staffRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    @Transactional
    public void createRequest(Long chefId, MenuItemUpdateRequestDto requestDto) {
        Staff chef = staffRepository.findById(chefId)
                .orElseThrow(() -> new RuntimeException("Chef not found with ID: " + chefId));
                
        MenuItem menuItem = menuItemRepository.findById(requestDto.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Menu item not found with ID: " + requestDto.getMenuItemId()));
                
        if (!chef.getBranch().getId().equals(menuItem.getBranch().getId())) {
            throw new RuntimeException("Chef can only create requests for menu items in their own branch.");
        }
                
        MenuItemUpdateRequest request = MenuItemUpdateRequest.builder()
                .chef(chef)
                .menuItem(menuItem)
                .chefNote(requestDto.getChefNote())
                .status(MenuItemUpdateRequestStatus.PENDING)
                .build();
                
        repository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemUpdateRequestResponseDto> getAllRequests(MenuItemUpdateRequestStatus status) {
        List<MenuItemUpdateRequest> requests;
        if (status != null) {
            requests = repository.findByStatus(status);
        } else {
            requests = repository.findAll();
        }
        
        return requests.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateRequestDecision(Long requestId, MenuItemUpdateDecisionDto decisionDto) {
        MenuItemUpdateRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with ID: " + requestId));
                
        request.setStatus(decisionDto.getStatus());
        request.setAdminNote(decisionDto.getAdminNote());
        
        repository.save(request);
    }
    
    private MenuItemUpdateRequestResponseDto mapToDto(MenuItemUpdateRequest request) {
        Staff chef = request.getChef();
        MenuItem item = request.getMenuItem();
        
        MenuCategory category = item.getCategory();
        String subCategory = item.getSubCategory();
        
        String chefName = "Unknown Chef";
        if (chef != null && chef.getUser() != null && chef.getUser().getFullName() != null) {
            chefName = chef.getUser().getFullName();
        } else if (chef != null) {
            chefName = (chef.getFirstName() != null ? chef.getFirstName() : "") + 
                       (chef.getLastName() != null ? " " + chef.getLastName() : "");
            if (chefName.trim().isEmpty()) {
                chefName = "Unknown Chef";
            }
        }
        
        return MenuItemUpdateRequestResponseDto.builder()
                .id(request.getId())
                .chefId(chef.getId())
                .chefName(chefName.trim())
                .menuItemId(item.getId())
                .menuItemName(item.getName())
                .menuCategory(category != null ? category.getName() : null)
                .menuSubCategory(subCategory)
                .menuItemImage(item.getImageUrl())
                .chefNote(request.getChefNote())
                .adminNote(request.getAdminNote())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
