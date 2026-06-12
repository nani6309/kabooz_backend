package com.kabooz.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents a single line-item within an {@link Order}.
 * All monetary fields are pre-calculated server-side by {@code PricingService}.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "bottle_type", nullable = false, length = 5)
    private BottleType bottleType;

    @Column(nullable = false, length = 100)
    private String flavor;

    /** Price per single bottle in INR (e.g. 20, 21, 22 …) */
    @Column(name = "price_per_bottle", nullable = false)
    private int pricePerBottle;

    /** Number of crates (GLASS) or cases (PET) ordered. */
    @Column(nullable = false)
    private int quantity;

    /** 24 for GLASS, 30 for PET */
    @Column(name = "bottles_per_unit", nullable = false)
    private int bottlesPerUnit;

    /** Tax-inclusive rate per crate/case */
    @Column(name = "rate_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerUnit;

    /** GST per crate/case (40% of taxable) */
    @Column(name = "tax_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxPerUnit;

    /** rate_per_unit = taxable_per_unit + tax_per_unit */
    @Column(name = "total_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPerUnit;

    /** taxable_per_unit × quantity */
    @Column(name = "taxable_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxableSubtotal;

    /** tax_per_unit × quantity */
    @Column(name = "tax_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxSubtotal;

    /** Grand total for this line (rate_per_unit × quantity) */
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    // ── Enum ─────────────────────────────────────────────────────

    public enum BottleType {
        GLASS, PET
    }
}
