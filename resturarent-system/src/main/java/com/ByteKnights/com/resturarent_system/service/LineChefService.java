package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefCookingStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefHistoryItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefItemDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.PagedResponse;

import java.util.List;

public interface LineChefService {
    List<LineChefItemDTO> getMyItems(String userEmail);
    void startItem(Long itemId, String userEmail);
    void completeItem(Long itemId, String userEmail);
    PagedResponse<LineChefHistoryItemDTO> getCookingHistory(String userEmail, int page, int size, String date, String status);
    LineChefCookingStatsDTO getCookingStats(String userEmail);
}
