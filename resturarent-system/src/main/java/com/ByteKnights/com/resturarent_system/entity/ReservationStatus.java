package com.ByteKnights.com.resturarent_system.entity;

public enum ReservationStatus {
    REQUESTED,   // Customer submitted, awaiting receptionist review
    CONFIRMED,   // Receptionist approved + assigned tables, awaiting payment
    REJECTED,    // Receptionist rejected the request
    PAID,        // Customer paid within deadline — reservation is locked in
    EXPIRED,     // Customer did not pay within the deadline
    CANCELLED,   // Cancelled by customer or receptionist
    COMPLETED    // Party arrived and was seated
}
