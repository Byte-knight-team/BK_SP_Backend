package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a Goods Receipt Note (GRN) — the record created when
 * the manager physically receives goods at the backdoor from a vendor.
 *
 * A GRN is always linked to an existing Purchase Order. One PO can have
 * multiple GRNs (e.g., partial deliveries over multiple days).
 *
 * When a GRN is confirmed, the system automatically creates
 * InventoryTransaction(RESTOCK) records and updates InventoryItem quantities
 * for all line items with condition = GOOD.
 */
@Entity
@Table(name = "goods_receipt_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Purchase Order this GRN is fulfilling (partially or fully). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /**
     * The vendor's invoice or delivery note reference number.
     * Optional — manager can leave blank if vendor doesn't provide one.
     * Stored for accounting and reconciliation purposes.
     */
    @Column(name = "invoice_reference", length = 100)
    private String invoiceReference;

    /** The staff member (manager) who physically received and confirmed the delivery. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_staff_id", nullable = false)
    private Staff receivedBy;

    /** The date and time the goods were received at the branch backdoor. */
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    /** Optional notes about the delivery (e.g., "Driver was late", "Packaging damaged"). */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        this.receivedAt = LocalDateTime.now();
    }
}
