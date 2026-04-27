package com.ByteKnights.com.resturarent_system.entity;

public enum MenuItemStatus {
    DRAFT,
    PENDING,
    ACTIVE,
    // Legacy value kept for backward compatibility with existing rows.
    APPROVED,
    REJECTED
}
