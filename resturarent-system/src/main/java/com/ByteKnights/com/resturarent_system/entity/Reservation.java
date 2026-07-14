package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A table reservation. In the customer-initiated model its status flows:
 * REQUESTED → CONFIRMED (tables assigned, pay clock started) → PAID → COMPLETED,
 * with REJECTED / EXPIRED / CANCELLED branches. One booking can span several tables
 * (the reservation_tables join) under a single customer.
 */
@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservations_customer_id", columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A reservation belongs to one branch (all its tables are in this branch).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    // A single booking can span multiple tables under one customer.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "reservation_tables",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "table_id")
    )
    @Builder.Default
    private Set<RestaurantTable> tables = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "reservation_time", nullable = false)
    private LocalDateTime reservationTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Column(name = "customer_note", length = 500)
    private String customerNote;

    @Column(name = "receptionist_note", length = 500)
    private String receptionistNote;

    // Total amount the customer must pay = timeCharge (feePerHour × hours × guests) + handlingFee.
    @Column(name = "total_charge", precision = 12, scale = 2)
    private java.math.BigDecimal totalCharge;

    // Non-refundable booking fee (kept even when the customer cancels their own paid booking).
    @Column(name = "handling_fee", precision = 12, scale = 2)
    private java.math.BigDecimal handlingFee;

    // How much was refunded when cancelled (null = no refund; set per the refund rules).
    @Column(name = "refund_amount", precision = 12, scale = 2)
    private java.math.BigDecimal refundAmount;

    // While CONFIRMED, the customer must pay before this instant or it auto-EXPIRES.
    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.REQUESTED;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
