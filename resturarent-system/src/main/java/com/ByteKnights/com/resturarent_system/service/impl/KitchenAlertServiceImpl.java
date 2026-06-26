package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateAlertRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;
import com.ByteKnights.com.resturarent_system.entity.KitchenAlert;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.KitchenAlertRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.KitchenAlertService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KitchenAlertServiceImpl implements KitchenAlertService {

    private final KitchenAlertRepository kitchenAlertRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Override
    @Transactional
    public void createKitchenAlert(CreateAlertRequestDTO dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        KitchenAlert alert = KitchenAlert.builder()
                .branch(staff.getBranch())
                .reportedBy(staff)
                .message(dto.getMessage())
                .type(dto.getType())
                .isResolved(false)
                .build();

        KitchenAlert savedAlert = kitchenAlertRepository.save(alert);

        ActiveAlertDTO alertDTO = new ActiveAlertDTO(
                savedAlert.getId(),
                savedAlert.getMessage(),
                savedAlert.getType(),
                "0m"
        );
        webSocketNotificationService.broadcastKitchenAlert(staff.getBranch().getId(), alertDTO);
    }

    @Override
    public List<ActiveAlertDTO> getActiveAlerts(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        List<KitchenAlert> alerts = kitchenAlertRepository.findByBranchIdAndIsResolvedFalseOrderByCreatedAtDesc(staff.getBranch().getId());

        List<ActiveAlertDTO> activeAlertDTOs = new ArrayList<>();

        for (KitchenAlert alert : alerts) {
            long minutes = Duration.between(alert.getCreatedAt(), LocalDateTime.now()).toMinutes();
            String timeAgo = minutes < 60 ? minutes + "m" : (minutes / 60) + "h";

            ActiveAlertDTO dto = new ActiveAlertDTO(
                    alert.getId(),
                    alert.getMessage(),
                    alert.getType(),
                    timeAgo
            );
            activeAlertDTOs.add(dto);
        }
        return activeAlertDTOs;
    }

    @Override
    @Transactional
    public void resolveAlert(Long alertId, String userEmail) {
        KitchenAlert alert = kitchenAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (alert.isResolved()) {
            throw new RuntimeException("This alert has already been resolved");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        alert.setResolved(true);
        alert.setResolvedBy(staff);
        alert.setResolvedAt(LocalDateTime.now());

        kitchenAlertRepository.save(alert);
    }
}
