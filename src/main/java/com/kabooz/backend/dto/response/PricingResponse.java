package com.kabooz.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response for the public pricing endpoint.
 * Frontend uses this to always render up-to-date prices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingResponse {

    private GlassPricing glass;
    private PetPricing pet;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlassPricing {
        private int bottlesPerCrate;
        private List<GlassPricePoint> prices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetPricing {
        private int bottlesPerCase;
        private List<PetPricePoint> prices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlassPricePoint {
        private int ppb;              // price per bottle
        private BigDecimal crateTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetPricePoint {
        private int ppb;
        private BigDecimal caseTotal;
    }
}
