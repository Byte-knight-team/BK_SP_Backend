package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;
import com.ByteKnights.com.resturarent_system.entity.KitchenAlert;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.KitchenAlertRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.ReceptionistAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionistAlertServiceImpl implements ReceptionistAlertService {

    private final KitchenAlertRepository kitchenAlertRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    @Override
    public List<ActiveAlertDTO> getAlerts(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        List<KitchenAlert> alerts = kitchenAlertRepository.findReceptionistAlerts(branchId, startOfToday);

        return alerts.stream()
                .map(a -> {
                    ActiveAlertDTO dto = new ActiveAlertDTO();
                    dto.setId(a.getId());
                    dto.setMessage(a.getMessage());
                    dto.setType(a.getType());
                    dto.setTimeAgo(computeTimeAgo(a.getCreatedAt()));
                    dto.setResolved(a.isResolved());
                    dto.setResolvedAt(a.getResolvedAt());
                    return dto;
                })
                .toList();
    }

    private String computeTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = duration.toHours();
        if (hours < 24) return hours + "h ago";
        long days = duration.toDays();
        return days + "d ago";
    }
}
