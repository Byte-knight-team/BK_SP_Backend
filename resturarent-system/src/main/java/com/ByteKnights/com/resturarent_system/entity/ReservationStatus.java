package com.ByteKnights.com.resturarent_system.entity;

public enum ReservationStatus {
    PENDING,     // reservation placed, upcoming (was CONFIRMED)
    CANCELLED,
    COMPLETED    // reserved party seated
}
