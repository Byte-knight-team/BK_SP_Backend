package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.admin.MenuItemUpdateDecisionDto;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemUpdateRequestDto;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemUpdateRequestResponseDto;
import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequestStatus;

import java.util.List;

public interface MenuItemUpdateRequestService {
    void createRequest(Long chefId, MenuItemUpdateRequestDto requestDto);
    List<MenuItemUpdateRequestResponseDto> getAllRequests(MenuItemUpdateRequestStatus status);
    void updateRequestDecision(Long requestId, MenuItemUpdateDecisionDto decisionDto);
}
