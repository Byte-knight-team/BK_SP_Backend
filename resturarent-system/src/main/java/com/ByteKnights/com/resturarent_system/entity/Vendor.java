package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing an external vendor/supplier that the branch purchases
 * goods from. Vendors are registered per-branch and are referenced in
 * Purchase Orders.
 */
@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The branch this vendor is registered under. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /** Trading name of the vendor (e.g., "FreshMart Suppliers"). */
    @Column(nullable = false, length = 150)
    private String name;

    /** Name of the main contact person at the vendor's side. */
    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    /** Phone number to contact the vendor for orders and follow-ups. */
    @Column(length = 30)
    private String phone;

    /** Optional email address for the vendor. */
    @Column(length = 150)
    private String email;

    /** Physical delivery or business address of the vendor. */
    @Column(length = 300)
    private String address;

    /**
     * Category of goods this vendor supplies (e.g., "Produce", "Dairy",
     * "Dry Goods", "Beverages"). Used for filtering in the Vendor Directory.
     */
    @Column(length = 100)
    private String category;

    /**
     * Whether this vendor is active and available for selection in new POs.
     * Soft-delete: deactivating a vendor preserves historical PO records.
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Timestamp when this vendor was first registered. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
