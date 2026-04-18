package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, length = 50)
    private String orderNumber;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "table_number")
    private String tableNumber;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PLACED;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private OrderType orderType;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private RestaurantTable tableObj;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Staff approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_chef_id")
    private Staff assignedChef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_delivery_id")
    private Staff assignedDelivery;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void recalculateTotal() {
        if(items != null) {
            this.total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        if(items == null) items = new ArrayList<>();
        items.add(item);
        item.setOrder(this);
    }
}
