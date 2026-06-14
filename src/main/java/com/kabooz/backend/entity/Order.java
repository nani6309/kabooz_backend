package com.kabooz.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sales order / invoice in the Kabooz system.
 * <p>
 * Tax breakdown: CGST 20% + SGST 20% = 40% total (tax-inclusive pricing).
 * All prices stored as tax-exclusive (taxable) + separate tax columns.
 * </p>
 * <p>
 * Review workflow: orders start as PENDING_REVIEW, then admin accepts (→ ACCEPTED,
 * invoice number assigned) or rejects (→ REJECTED, no invoice created).
 * </p>
 */
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

    /** Null until the order is accepted by admin. */
    @Column(name = "invoice_no", unique = true, length = 20)
    private String invoiceNo;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    /** CGST @ 20% of taxable amount */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cgst = BigDecimal.ZERO;

    /** SGST @ 20% of taxable amount */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "received_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    /** Whether this order should display a GST number on the invoice. */
    @Column(name = "with_gst", nullable = false)
    @Builder.Default
    private Boolean withGst = false;

    /** Optional customer GST number (GSTIN) printed only when GST is enabled. */
    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    /** Optional customer shop/business name printed on the invoice. */
    @Column(name = "customer_shop_name", length = 160)
    private String customerShopName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private OrderSource source = OrderSource.HOMEPAGE;

    /**
     * Review workflow status.
     * Orders start as PENDING_REVIEW; admin then accepts (invoice assigned) or rejects.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.PENDING_REVIEW;

    /** Optional reason provided by admin when rejecting an order. */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** Soft-delete timestamp. Non-null means logically deleted. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // ── Convenience ──────────────────────────────────────────────

    /**
     * Adds an item and wires the back-reference.
     *
     * @param item the order item to add
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    // ── Enums ────────────────────────────────────────────────────

    public enum OrderStatus {
        PENDING, PAID, OVERDUE
    }

    public enum OrderSource {
        HOMEPAGE, ADMIN
    }

    public enum ReviewStatus {
        PENDING_REVIEW, ACCEPTED, REJECTED
    }
}
