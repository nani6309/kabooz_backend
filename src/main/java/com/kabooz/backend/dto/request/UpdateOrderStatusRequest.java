package com.kabooz.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for updating an order's payment status.
 */
@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private String status;   // PENDING | PAID | OVERDUE

    @DecimalMin(value = "0.0", inclusive = true, message = "Received amount cannot be negative")
    private BigDecimal receivedAmount;
}
