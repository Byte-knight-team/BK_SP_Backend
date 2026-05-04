package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;import java.util.List;

public interface ReceptionistTableService {

    List<ReceptionistTableResponse> getBranchTables(String userEmail);

    void occupyTable(Long tableId, Integer guestCount, String userEmail);

    void clearTable(Long tableId, String userEmail);

}
