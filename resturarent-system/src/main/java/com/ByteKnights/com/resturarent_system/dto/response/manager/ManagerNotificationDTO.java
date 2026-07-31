package com.ByteKnights.com.resturarent_system.dto.response.manager;

import com.ByteKnights.com.resturarent_system.entity.ManagerNotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ManagerNotificationDTO {
    private Long id;
    private ManagerNotificationType type;
    private String message;
    private boolean isRead;
    private Long referenceId;
    private LocalDateTime createdAt;
}
