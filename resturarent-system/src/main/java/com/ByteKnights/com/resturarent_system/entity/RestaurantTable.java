package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * A physical table in a branch. (branch_id + table_number) is unique per branch.
 * Its `state` is the live floor status the receptionist manages.
 */
@Entity
@Table(name = "restaurant_tables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"branch_id", "table_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The branch this table physically belongs to.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // Human-facing table number (unique within the branch).
    @Column(name = "table_number", nullable = false)
    private Integer tableNumber;

    // How many seats the table has.
    private Integer capacity;

    // Live floor status: AVAILABLE → RESERVED (held for a booking) → OCCUPIED (guests seated).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TableStatus state = TableStatus.AVAILABLE;

    // Guests currently seated (0 when not occupied).
    @Builder.Default
    @Column(name = "current_guest_count")
    private Integer currentGuestCount = 0;

    // Number of active orders on this table.
    @Builder.Default
    @Column(name = "active_order_count")
    private Integer activeOrderCount = 0;

    // When the current occupancy came from seating a reservation, this holds that reservation's id
    // (so the card/modal can show "Occupied for reservation …" and blink when its time is up).
    // Null for walk-in occupancy. Cleared when the table is cleared.
    @Column(name = "seated_reservation_id")
    private Long seatedReservationId;

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
