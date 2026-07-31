package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity representing a single received item line within a Goods Receipt Note.
 * Each GrnLineItem corresponds to one PurchaseOrderItem and records the actual
 * quantity received, the condition of the goods, and any discrepancy notes.
 *
 * Key business rules (enforced in the service layer):
 * - Only items with condition = GOOD trigger an InventoryTransaction(RESTOCK).
 * - DAMAGED and REJECTED items do NOT update inventory.
 * - If receivedQuantity differs from the PO's orderedQuantity, a discrepancy
 *   note is automatically generated and stored.
 */
@Entity
@Table(name = "grn_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrnLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Goods Receipt Note this line item belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_note_id", nullable = false)
    private GoodsReceiptNote goodsReceiptNote;

    /** The corresponding Purchase Order line item being fulfilled. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id", nullable = false)
    private PurchaseOrderItem purchaseOrderItem;

    /**
     * The actual quantity physically received at the backdoor.
     * May differ from PurchaseOrderItem.orderedQuantity (short delivery, surplus).
     */
    @Column(name = "received_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal receivedQuantity;

    /**
     * The physical condition of the received goods.
     * GOOD     → quantity added to inventory stock.
     * DAMAGED  → quantity NOT added; flagged for vendor follow-up.
     * REJECTED → quantity NOT added; goods refused and returned.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "item_condition", nullable = false, length = 20)
    private GrnItemCondition condition = GrnItemCondition.GOOD;

    /**
     * Auto-generated or manually entered note describing any discrepancy.
     * Example: "Ordered 50 kg, received 45 kg — 5 kg short."
     * Null if received quantity matches ordered quantity and condition is GOOD.
     */
    @Column(name = "discrepancy_note", columnDefinition = "TEXT")
    private String discrepancyNote;
}
