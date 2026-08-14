package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity representing a single line item within a Purchase Order.
 * Each line item specifies one product being ordered from the vendor.
 *
 * The link to InventoryItem is nullable because:
 * - If the item already exists in the catalog → inventoryItem is set.
 * - If the item is new and not yet in the catalog → inventoryItem is null
 *   at PO creation time, and must be created/linked before a GRN can be
 *   completed for that line.
 * The itemNameSnapshot always captures the plain-text name at PO time
 * regardless of catalog state, ensuring a durable audit trail.
 */
@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Purchase Order this line item belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /**
     * Link to the inventory catalog item.
     * Nullable — null when the item did not exist in the catalog at PO creation.
     * Must be populated (inline catalog creation) before a GRN can restock this item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = true)
    private InventoryItem inventoryItem;

    /**
     * Plain-text name of the item at the time the PO was created.
     * Stored independently from InventoryItem.name to preserve the audit
     * trail even if the catalog item is renamed or deleted later.
     */
    @Column(name = "item_name_snapshot", nullable = false, length = 150)
    private String itemNameSnapshot;

    /** The quantity ordered from the vendor. */
    @Column(name = "ordered_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderedQuantity;

    /** Unit of measurement (e.g., "kg", "Pcs", "Litres", "Bottles"). */
    @Column(nullable = false, length = 20)
    private String unit;

    /** The agreed unit price with the vendor at time of ordering. */
    @Column(name = "agreed_unit_price", precision = 10, scale = 2)
    private BigDecimal agreedUnitPrice;
}
