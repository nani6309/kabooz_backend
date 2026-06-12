package com.kabooz.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for PATCH /api/admin/orders/{id}/gst.
 * <p>
 * Payload shape:
 * <pre>
 *   { "gstNumber": "29ABCDE1234F1Z5" }   // attach / update GSTIN
 *   { "gstNumber": null }                 // clear GSTIN (mark order Without GST)
 * </pre>
 * A blank string is treated the same as {@code null}.
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UpdateOrderGstRequest {

    @Pattern(regexp = "(?i)^[0-9a-z]{15}$", message = "GST number must be a valid 15-character GSTIN")
    private String gstNumber;
}

