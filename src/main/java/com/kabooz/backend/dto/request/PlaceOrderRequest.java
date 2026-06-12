package com.kabooz.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for placing an order from the public homepage.
 * All pricing is recalculated server-side; no totals are trusted from the client.
 */
@Data
public class PlaceOrderRequest {

    @Valid
    @NotNull(message = "Customer information is required")
    private CustomerInfo customer;

    @Valid
    @NotNull(message = "Items list is required")
    @Size(min = 1, max = 20, message = "Order must have 1–20 items")
    private List<OrderItemRequest> items;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    // ── Inner DTOs ────────────────────────────────────────────────

    @Data
    public static class CustomerInfo {

        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 100, message = "Name must be 2–100 characters")
        private String name;

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Mobile must be exactly 10 numeric digits")
        private String mobile;

        @Size(max = 500, message = "Address must not exceed 500 characters")
        private String address;

        @Size(max = 100)
        private String placeOfSupply = "Karnataka";

        @Size(max = 160, message = "Shop name must not exceed 160 characters")
        private String customerShopName;
    }

    @Data
    public static class OrderItemRequest {

        @NotNull(message = "Bottle type is required (GLASS or PET)")
        private String bottleType;  // GLASS | PET

        @NotBlank(message = "Flavor is required")
        @Size(min = 1, max = 100, message = "Flavor must be 1–100 characters")
        private String flavor;

        @NotNull(message = "Price per bottle is required")
        @Min(value = 1, message = "Price per bottle must be positive")
        private Integer pricePerBottle;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 500, message = "Quantity must not exceed 500")
        private Integer quantity;
    }
}
