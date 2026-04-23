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

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}