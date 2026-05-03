package com.ByteKnights.com.resturarent_system.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kitchen_alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KitchenAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The core connection: Filter alerts by branch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // Track the person who reported it
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private Staff reportedBy;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    private KitchenAlertType type; // CRITICAL, WARNING, INFO

    @Column(nullable = false)
    private boolean isResolved = false;

    private LocalDateTime createdAt;

    // Optional: Tracking the fix
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private Staff resolvedBy;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

