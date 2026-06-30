package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefItemDTO;

import java.util.List;

public interface LineChefService {
    List<LineChefItemDTO> getMyItems(String userEmail);
    void startItem(Long itemId, String userEmail);
    void completeItem(Long itemId, String userEmail);
}
