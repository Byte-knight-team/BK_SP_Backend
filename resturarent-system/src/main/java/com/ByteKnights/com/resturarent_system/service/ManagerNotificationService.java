package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerNotificationDTO;
import com.ByteKnights.com.resturarent_system.entity.ManagerNotificationType;

import java.util.List;

public interface ManagerNotificationService {
    void createNotification(Long branchId, ManagerNotificationType type, String message, Long referenceId);
    List<ManagerNotificationDTO> getUnreadNotifications(Long branchId);
    void markAsRead(Long notificationId);
}
