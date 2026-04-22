package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_enabled", nullable = false)
    private boolean taxEnabled = false;

    @Column(name = "tax_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(name = "service_charge_enabled", nullable = false)
    private boolean serviceChargeEnabled = false;

    @Column(name = "service_charge_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal serviceChargePercentage = BigDecimal.ZERO;

    @Column(name = "loyalty_enabled", nullable = false)
    private boolean loyaltyEnabled = false;

    @Column(name = "points_per_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal pointsPerAmount = BigDecimal.ZERO;

    @Column(name = "amount_per_point", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountPerPoint = BigDecimal.ONE;

    @Column(name = "min_points_to_redeem", nullable = false)
    private Integer minPointsToRedeem = 0;

    @Column(name = "value_per_point", precision = 10, scale = 2, nullable = false)
    private BigDecimal valuePerPoint = BigDecimal.ZERO;

    @Column(name = "order_cancel_window_minutes", nullable = false)
    private Integer orderCancelWindowMinutes = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}