package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "menu_item_update_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemUpdateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_id", nullable = false)
    private Staff chef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "chef_note", length = 500)
    private String chefNote;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MenuItemUpdateRequestStatus status = MenuItemUpdateRequestStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
