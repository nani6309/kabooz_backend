package com.kabooz.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Compact order summary for list/pagination views.
 * Used in GET /api/admin/orders paginated responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {

    private Long id;
    private String invoiceNo;
    private String customerName;
    private String customerShopName;
    private String mobile;
    private LocalDate invoiceDate;
    private BigDecimal grandTotal;
    private BigDecimal receivedAmount;
    private BigDecimal balanceDue;
    private String status;
    private Boolean withGst;
    private String gstNumber;
    private int itemCount;
    private LocalDateTime createdAt;
}
