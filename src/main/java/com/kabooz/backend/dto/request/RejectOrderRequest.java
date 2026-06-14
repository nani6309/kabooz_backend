package com.kabooz.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for POST /api/admin/orders/{id}/reject.
 * The rejection reason is optional.
 */
@Data
public class RejectOrderRequest {

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    private String reason;
}
