package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a chef's request for inventory restocking.
 * Chefs submit these requests when they notice stock is running low,
 * and managers can approve or reject them from the Inventory Management page.
 */

@Entity
@Table(name = "chef_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The branch associated with this entity.
     * Mapped as a many-to-one relationship using lazy loading to optimize
     * performance.
     * The database column 'branch_id' acts as a mandatory (not-null) foreign key.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // Full name of the chef who submitted this request
    @Column(name = "chef_name", nullable = false, length = 150)
    private String chefName;

    // Name of the inventory item being requested
    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    // Amount of the item requested by the chef
    @Column(name = "requested_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedQuantity;

    // Unit of measurement for the requested quantity (e.g., "kg", "Pcs", "Liters")
    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    // Additional notes or comments from the chef
    @Column(name = "chef_note", length = 500)
    private String chefNote;

    /**
     * Current status of this request.
     * Defaults to PENDING when created; managers can change it to APPROVED or
     * REJECTED.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private ChefRequestStatus status = ChefRequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private ChefRequestType requestType;


    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Lifecycle callback: automatically sets the createdAt timestamp
     * when the entity is first persisted to the database.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}