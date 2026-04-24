package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PLACED;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private OrderType orderType;

    // ==========================================
    // ORDER-SPECIFIC CONTACT DETAILS
    // ==========================================
    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
    
    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    // ==========================================
    // MONEY, FEES, AND DISCOUNTS
    // ==========================================
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "service_charge", precision = 10, scale = 2)
    private BigDecimal serviceCharge = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    // ==========================================
    // COUPON & LOYALTY TRACKING (For Refunds)
    // ==========================================
    @Column(name = "applied_coupon_code", length = 50)
    private String appliedCouponCode;

    @Column(name = "reward_points_earned")
    private Integer rewardPointsEarned = 0;

    @Column(name = "reward_points_redeemed")
    private Integer rewardPointsRedeemed = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // ==========================================
    // RELATIONS
    // ==========================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Staff approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "kitchen_notes")
    private String kitchenNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_chef_id")
    private Staff assignedChef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_delivery_id")
    private Staff assignedDelivery;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // ==========================================
    // TIMESTAMPS & KITCHEN METRICS
    // ==========================================
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @Column(name = "cooking_started_at")
    private LocalDateTime cookingStartedAt;

    @Column(name = "cooking_completed_at")
    private LocalDateTime cookingCompletedAt;

    public Order() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (statusUpdatedAt == null) statusUpdatedAt = LocalDateTime.now();
        
        // Safety checks for older code that might not set these
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
        if (deliveryFee == null) deliveryFee = BigDecimal.ZERO;
        if (serviceCharge == null) serviceCharge = BigDecimal.ZERO;
        if (rewardPointsEarned == null) rewardPointsEarned = 0;
        if (rewardPointsRedeemed == null) rewardPointsRedeemed = 0;
        
        recalculateTotal(); 
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
        this.statusUpdatedAt = LocalDateTime.now();

        if (newStatus == OrderStatus.PENDING) {
            this.approvedAt = LocalDateTime.now();
        } else if (newStatus == OrderStatus.PREPARING) {
            this.cookingStartedAt = LocalDateTime.now();
        } else if (newStatus == OrderStatus.COMPLETED) {
            this.cookingCompletedAt = LocalDateTime.now();
        }
    }

    public void recalculateTotal() {
        // 1. Calculate base items total
        if (items != null) {
            this.totalAmount = items.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            this.totalAmount = BigDecimal.ZERO;
        }

        // 2. Ensure no null fees
        if (this.discountAmount == null) this.discountAmount = BigDecimal.ZERO;
        if (this.taxAmount == null) this.taxAmount = BigDecimal.ZERO;
        if (this.deliveryFee == null) this.deliveryFee = BigDecimal.ZERO;
        if (this.serviceCharge == null) this.serviceCharge = BigDecimal.ZERO;

        // 3. Final Math: Items - Discount + Tax + Delivery + Service
        this.finalAmount = this.totalAmount
                .subtract(this.discountAmount)
                .add(this.taxAmount)
                .add(this.deliveryFee)
                .add(this.serviceCharge);

        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        item.setOrder(this);
    }
}