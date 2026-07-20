package com.ByteKnights.com.resturarent_system.entity;

public enum PurchaseOrderStatus {
    DRAFT,               // PO created but not yet submitted
    SUBMITTED,           // PO submitted internally — manager contacts vendor externally
    PARTIALLY_RECEIVED,  // Some items received via GRN, others still pending
    RECEIVED,            // All items fully received
    CANCELLED            // PO was cancelled
}
