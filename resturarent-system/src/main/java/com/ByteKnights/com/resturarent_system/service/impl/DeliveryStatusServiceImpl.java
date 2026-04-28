package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.DeliveryStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryStatusServiceImpl implements DeliveryStatusService {

    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public void toggleOnlineStatus(Long userId, boolean online) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found for user ID: " + userId));
        
        staff.setOnline(online);
        staffRepository.save(staff);
    }

    @Override
    public boolean getOnlineStatus(Long userId) {
        return staffRepository.findByUserId(userId)
                .map(Staff::isOnline)
                .orElse(false);
    }
}
