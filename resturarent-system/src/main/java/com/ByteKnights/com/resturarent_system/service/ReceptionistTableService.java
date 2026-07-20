package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;
import java.util.List;

/**
 * Table-floor operations for the receptionist's own branch (the Table Management page).
 * These manage the physical table state; reservation lifecycle lives in
 * {@link ReceptionistReservationService}.
 */
public interface ReceptionistTableService {

    // All tables in the receptionist's branch, with live state + orders + today's reservations.
    List<ReceptionistTableResponse> getBranchTables(String userEmail);

    // Walk-in seating: mark an AVAILABLE/RESERVED table OCCUPIED (no reservation link).
    void occupyTable(Long tableId, Integer guestCount, String userEmail);

    // Change only the guest count of an already-occupied table (keeps the seated timer running).
    void updateGuestCount(Long tableId, Integer guestCount, String userEmail);

    // Guests left: return the table to AVAILABLE (or RESERVED if another booking is imminent).
    void clearTable(Long tableId, String userEmail);

}
