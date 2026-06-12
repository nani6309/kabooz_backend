package com.kabooz.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer who places an order.
 * Customers are identified by their unique mobile number (upsert on mobile).
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Exactly 10 numeric digits — unique per customer. */
    @Column(nullable = false, unique = true, length = 10)
    private String mobile;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "place_of_supply", nullable = false, length = 100)
    @Builder.Default
    private String placeOfSupply = "Karnataka";

    /** Optional customer GSTIN (15 chars). Null/blank means "no GST". */
    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();
}
