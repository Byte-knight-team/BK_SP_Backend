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

    /** Automated stock reduction when a customer order is successfully placed. */
    ORDER_DEDUCT,

    /** Automated stock restocking when an order is cancelled or rejected. */
    ORDER_REFUND
}
