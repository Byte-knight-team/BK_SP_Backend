package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerNotificationDTO;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.ManagerNotification;
import com.ByteKnights.com.resturarent_system.entity.ManagerNotificationType;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.ManagerNotificationRepository;
import com.ByteKnights.com.resturarent_system.service.ManagerNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerNotificationServiceImpl implements ManagerNotificationService {

    private final ManagerNotificationRepository notificationRepository;
    private final BranchRepository branchRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(Long branchId, ManagerNotificationType type, String message, Long referenceId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));

        ManagerNotification notification = ManagerNotification.builder()
                .branch(branch)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);

        /*
         * Send the WebSocket ping AFTER the REQUIRES_NEW transaction commits.
         * This guarantees the notification row is visible in the DB before the
         * frontend re-fetches, preventing a race condition where the client
         * would re-fetch and see 0 notifications because the row wasn't committed yet.
         */
        final String destination = "/topic/branch/" + branchId + "/manager-notifications";
        final java.util.Map<String, String> payload = java.util.Map.of(
                "message", "NEW_NOTIFICATION",
                "type", type.name()
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        messagingTemplate.convertAndSend(destination, payload);
                        log.info("[ManagerNotification] WebSocket ping sent to {} after commit", destination);
                    } catch (Exception ex) {
                        log.error("[ManagerNotification] Failed to send WebSocket ping after commit: {}", ex.getMessage(), ex);
                    }
                }
            });
        } else {
            // No active transaction (e.g. called from a test or async context) — send immediately
            messagingTemplate.convertAndSend(destination, payload);
            log.info("[ManagerNotification] WebSocket ping sent immediately (no active tx) to {}", destination);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerNotificationDTO> getUnreadNotifications(Long branchId) {
        return notificationRepository.findByBranchIdAndIsReadFalseOrderByCreatedAtDesc(branchId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        ManagerNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        
        notification.setRead(true);
        notificationRepository.save(notification);
    }
    
    @Override
    public void pingNotificationResolved(Long branchId) {
        String destination = "/topic/branch/" + branchId + "/manager-notifications";
        messagingTemplate.convertAndSend(destination, java.util.Map.of("message", "NOTIFICATION_RESOLVED"));
    }

    private ManagerNotificationDTO mapToDTO(ManagerNotification entity) {
        return ManagerNotificationDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .message(entity.getMessage())
                .isRead(entity.isRead())
                .referenceId(entity.getReferenceId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
