package com.kabooz.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a distributor enquiry submitted from the public homepage.
 * Lifecycle status: NEW → CONTACTED → APPROVED | REJECTED.
 */
@Entity
@Table(name = "distributors", indexes = {
        @Index(name = "idx_distributor_status", columnList = "status"),
        @Index(name = "idx_distributor_mobile", columnList = "mobile"),
        @Index(name = "idx_distributor_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Distributor {

    public enum Status {
        NEW, CONTACTED, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    /** Indian mobile (10 digits, must start with 6-9). */
    @Column(nullable = false, length = 15)
    private String mobile;

    @Column(name = "shop_name", nullable = false, length = 160)
    private String shopName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.NEW;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}

