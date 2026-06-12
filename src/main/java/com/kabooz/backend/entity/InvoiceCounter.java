package com.kabooz.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Single-row table that tracks the last-used invoice sequence number.
 * Increments are performed with {@code SELECT ... FOR UPDATE} to prevent
 * concurrent duplicates.
 */
@Entity
@Table(name = "invoice_counter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCounter {

    @Id
    @Column(nullable = false)
    private int id;

    // `last_value` is a reserved word in MySQL 8; PostgreSQL does not require quoting.
    @Column(name = "last_value", nullable = false)
    private int lastValue;
}
