package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "branch_config",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_branch_config_branch", columnNames = "branch_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class BranchConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "delivery_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "delivery_enabled", nullable = false)
    private boolean deliveryEnabled = true;

    @Column(name = "pickup_enabled", nullable = false)
    private boolean pickupEnabled = true;

    @Column(name = "dine_in_enabled", nullable = false)
    private boolean dineInEnabled = true;

    @Column(name = "branch_active_for_orders", nullable = false)
    private boolean branchActiveForOrders = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Reservation settings
    @Column(name = "reservation_fee_per_hour", precision = 10, scale = 2, nullable = false)
    private BigDecimal reservationFeePerHour = new BigDecimal("1000.00");

    @Column(name = "reservation_handling_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal reservationHandlingFee = new BigDecimal("500.00");

    @Column(name = "reservation_payment_window_minutes", nullable = false)
    private Integer reservationPaymentWindowMinutes = 30;

    @Column(name = "reservation_min_lead_hours", nullable = false)
    private Integer reservationMinLeadHours = 3;

    @Column(name = "reservation_max_guest_count", nullable = false)
    private Integer reservationMaxGuestCount = 20;

    @Column(name = "reservations_enabled", nullable = false)
    private boolean reservationsEnabled = true;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getReservationFeePerHour() {
        return reservationFeePerHour != null ? reservationFeePerHour : new BigDecimal("1000.00");
    }

    public BigDecimal getReservationHandlingFee() {
        return reservationHandlingFee != null ? reservationHandlingFee : new BigDecimal("500.00");
    }

    public Integer getReservationPaymentWindowMinutes() {
        return reservationPaymentWindowMinutes != null ? reservationPaymentWindowMinutes : 30;
    }

    public Integer getReservationMinLeadHours() {
        return reservationMinLeadHours != null ? reservationMinLeadHours : 3;
    }

    public Integer getReservationMaxGuestCount() {
        return reservationMaxGuestCount != null ? reservationMaxGuestCount : 20;
    }
}