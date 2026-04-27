package com.ByteKnights.com.resturarent_system.entity;

/**
 * Defines the type of inventory transaction performed.
 * This helps in categorizing the stock movements for reports and audits.
 */
public enum InventoryTransactionType {
    /** Stock added via manual manager update or supplier delivery. */
    RESTOCK,

    /** Stock removed due to spoilage, damage, or expiration. */
    WASTAGE,

    /** Stock adjusted to fix data entry errors from previous updates. */
    CORRECTION,

    /** Stock consumed by the kitchen for orders (reserved for future use). */
    CONSUMPTION
}
