package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.request.admin.MenuItemUpdateDecisionDto;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemUpdateRequestDto;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemUpdateRequestResponseDto;
import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequestStatus;
import com.ByteKnights.com.resturarent_system.service.MenuItemUpdateRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MenuItemUpdateRequestController {

    private final MenuItemUpdateRequestService service;

    @PostMapping("/kitchen/menu-item-requests")
    public ResponseEntity<String> createRequest(
            @RequestParam Long chefId,
            @RequestBody MenuItemUpdateRequestDto requestDto) {
        service.createRequest(chefId, requestDto);
        return ResponseEntity.ok("Menu item update request created successfully.");
    }

    @GetMapping("/admin/menu-item-requests")
    public ResponseEntity<List<MenuItemUpdateRequestResponseDto>> getAllRequests(
            @RequestParam(required = false) MenuItemUpdateRequestStatus status) {
        return ResponseEntity.ok(service.getAllRequests(status));
    }

    @PutMapping("/admin/menu-item-requests/{id}/decision")
    public ResponseEntity<String> updateRequestDecision(
            @PathVariable Long id,
            @RequestBody MenuItemUpdateDecisionDto decisionDto) {
        service.updateRequestDecision(id, decisionDto);
        return ResponseEntity.ok("Menu item update request decision recorded.");
    }
}
