package com.ByteKnights.com.resturarent_system.service;

public interface DeliveryStatusService {
    void toggleOnlineStatus(Long userId, boolean online);
    boolean getOnlineStatus(Long userId);
}
