package com.ByteKnights.com.resturarent_system.entity;

public enum KitchenAlertType {
    CRITICAL, // Severe issues that halt kitchen operations (e.g., equipment failure, gas leak)
    WARNING, // Non-blocking issues that require attention (e.g., low stock, minor delays)
    INFO // General updates or notifications for the receptionist (e.g., milestone reached, status updates)
}

