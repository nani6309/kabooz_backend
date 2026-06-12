package com.kabooz.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full order response including customer details, all line items,
 * and complete tax breakdown. Used for GET /api/admin/orders/{id}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String status;
    private String source;
    private String notes;
    private LocalDateTime createdAt;

    // ── Customer ──────────────────────────────────────────────────
    private CustomerDto customer;

    // ── Items ─────────────────────────────────────────────────────
    private List<OrderItemDto> items;

    // ── Financials ────────────────────────────────────────────────
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal grandTotal;
    private BigDecimal receivedAmount;
    private BigDecimal balanceDue;
    private Boolean withGst;
    private String gstNumber;

    // ── Inner DTOs ────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDto {
        private Long id;
        private String name;
        private String mobile;
        private String address;
        private String placeOfSupply;
        private String gstNumber;
        private String customerShopName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private Long id;
        private String bottleType;
        private String flavor;
        private int pricePerBottle;
        private int quantity;
        private int bottlesPerUnit;
        private BigDecimal ratePerUnit;
        private BigDecimal taxPerUnit;
        private BigDecimal totalPerUnit;
        private BigDecimal taxableSubtotal;
        private BigDecimal taxSubtotal;
        private BigDecimal lineTotal;
    }
}
