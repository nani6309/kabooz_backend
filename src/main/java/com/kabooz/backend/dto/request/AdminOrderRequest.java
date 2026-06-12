package com.kabooz.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for admin-created orders.
 * Admin can supply pricePerBottle freely (not restricted to catalogue), set
 * receivedAmount, dueDate, and notes manually.
 */
@Data
public class AdminOrderRequest {

    @Valid
    @NotNull(message = "Customer information is required")
    private CustomerInfo customer;

    @Valid
    @NotNull(message = "Items list is required")
    @Size(min = 1, max = 20, message = "Order must have 1–20 items")
    private List<OrderItemRequest> items;

    private BigDecimal receivedAmount;

    private LocalDate dueDate;

    private Boolean withGst = false;

    @Pattern(regexp = "(?i)^[0-9a-z]{15}$", message = "GST number must be a valid 15-character GSTIN")
    private String gstNumber;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @AssertTrue(message = "GST number is required when GST is enabled")
    public boolean isGstConfigurationValid() {
        if (!Boolean.TRUE.equals(withGst)) {
            return true;
        }
        return gstNumber != null && !gstNumber.isBlank();
    }

    // ── Inner DTOs ────────────────────────────────────────────────

    @Data
    public static class CustomerInfo {

        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 100)
        private String name;

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Mobile must be exactly 10 numeric digits")
        private String mobile;

        @Size(max = 500)
        private String address;

        @Size(max = 100)
        private String placeOfSupply = "Karnataka";

        @Pattern(regexp = "(?i)^[0-9a-z]{15}$", message = "GST number must be a valid 15-character GSTIN")
        private String gstNumber;

        @Size(max = 160, message = "Shop name must not exceed 160 characters")
        private String customerShopName;
    }

    @Data
    public static class OrderItemRequest {

        @NotNull(message = "Bottle type is required (GLASS or PET)")
        private String bottleType;

        @NotBlank(message = "Flavor is required")
        @Size(min = 1, max = 100)
        private String flavor;

        @NotNull(message = "Price per bottle is required")
        @Min(1)
        private Integer pricePerBottle;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 500, message = "Quantity must not exceed 500")
        private Integer quantity;
    }
}
