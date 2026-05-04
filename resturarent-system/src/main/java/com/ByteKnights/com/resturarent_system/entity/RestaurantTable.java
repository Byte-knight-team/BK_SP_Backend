package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_tables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"branch_id", "table_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Represents a physical table in a branch.
 */
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "table_number", nullable = false)
    private Integer tableNumber;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TableStatus state = TableStatus.AVAILABLE;

    @Builder.Default
    @Column(name = "current_guest_count")
    private Integer currentGuestCount = 0;

    @Builder.Default
    @Column(name = "active_order_count")
    private Integer activeOrderCount = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt = LocalDateTime.now();


    /**
     * Sets creation timestamp when a row is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.statusUpdatedAt = LocalDateTime.now(); // Set initial status time
    }
}
