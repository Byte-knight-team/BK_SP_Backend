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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
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

        // Ping the manager frontend so it knows to re-fetch the notifications
        String destination = "/topic/branch/" + branchId + "/manager-notifications";
        log.info("Pinging manager notifications at {}: {}", destination, message);
        messagingTemplate.convertAndSend(destination, java.util.Map.of("message", "NEW_NOTIFICATION"));
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
