package com.ByteKnights.com.resturarent_system.entity;

public enum ChefRequestStatus {
    PENDING, // Awaiting manager approval
    APPROVED, // Manager approved the request
    ORDERED, // Manager created a Purchase Order for this request
    REJECTED // Manager rejected the request
}
