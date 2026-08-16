package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit;

    @Column(name = "reorder_level", precision = 10, scale = 2)
    private BigDecimal reorderLevel;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // The minimum quantity that must be kept on hand to run a full day of kitchen operations
    // (a "par level" — the target amount stock gets restocked back up to).
    @Column(name = "daily_required_stock", precision = 10, scale = 2)
    private BigDecimal dailyRequiredStock;

    @Column(length = 50)
    private String category;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // Tracks whether a LOW/CRITICAL alert has already been broadcast for the current dip, so
    // repeated deductions while stock stays below reorder level don't spam a toast every time.
    // Reset to false once stock is restocked back above reorder level.
    @Column(name = "low_stock_alerted")
    @Builder.Default
    private boolean lowStockAlerted = false;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Same threshold logic used across inventory alert screens: at/below reorder level is LOW,
     * at/below half the reorder level is CRITICAL, otherwise OK.
     */
    public String computeStockLevel() {
        if (quantity == null || reorderLevel == null) return "OK";
        double current = quantity.doubleValue();
        double reorder = reorderLevel.doubleValue();
        if (current <= reorder) {
            return (current <= reorder / 2) ? "CRITICAL" : "LOW";
        }
        return "OK";
    }
}
