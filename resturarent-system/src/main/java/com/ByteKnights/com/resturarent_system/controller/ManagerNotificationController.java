package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerNotificationDTO;
import com.ByteKnights.com.resturarent_system.service.ManagerNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/notifications")
@RequiredArgsConstructor
public class ManagerNotificationController {

    private final ManagerNotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ManagerNotificationDTO>> getUnreadNotifications(
            @RequestParam Long branchId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(branchId));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
