package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a single stock movement or update event in the inventory.
 * This serves as an audit trail for all changes made to inventory items.
 */
@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The inventory item that was updated. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    /** The staff member (manager) who performed the update. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    /** The type of update performed (RESTOCK, WASTAGE, CORRECTION). */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private InventoryTransactionType transactionType;

    /** The amount of quantity added or removed during this transaction. */
    @Column(name = "quantity_change", precision = 10, scale = 2)
    private BigDecimal quantityChange;

    /** The stock level of the item BEFORE this transaction occurred. */
    @Column(name = "previous_quantity", precision = 10, scale = 2)
    private BigDecimal previousQuantity;

    /** The stock level of the item AFTER this transaction was completed. */
    @Column(name = "new_quantity", precision = 10, scale = 2)
    private BigDecimal newQuantity;

    /** The unit price recorded at the time of the transaction. */
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** 
     * Detailed notes or reasons for the transaction. 
     * This is where restock notes, wastage reasons, and correction explanations are stored. 
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** The timestamp when this transaction was recorded. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
