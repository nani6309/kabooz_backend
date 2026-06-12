package com.kabooz.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight response returned immediately after a public order submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderResponse {

    private Long id;
    private String invoiceNo;
    private BigDecimal grandTotal;
    private String status;
    private String message;
}
