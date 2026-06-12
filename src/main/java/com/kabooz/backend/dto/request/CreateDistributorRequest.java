package com.kabooz.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for creating a distributor enquiry from the public homepage.
 */
@Data
public class CreateDistributorRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be at most 120 characters")
    private String name;

    @NotBlank(message = "Mobile is required")
    @Pattern(regexp = "^[6-9]\\d{9}$",
            message = "Mobile must be a valid 10-digit Indian number starting with 6-9")
    private String mobile;

    @NotBlank(message = "Shop name is required")
    @Size(max = 160, message = "Shop name must be at most 160 characters")
    private String shopName;

    @NotBlank(message = "Address is required")
    private String address;
}

