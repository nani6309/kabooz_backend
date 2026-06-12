package com.kabooz.backend.service;

import com.kabooz.backend.dto.response.PricingResponse;
import com.kabooz.backend.entity.OrderItem;
import com.kabooz.backend.entity.OrderItem.BottleType;
import com.kabooz.backend.exception.InvalidPricingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service encapsulating all Kabooz pricing rules.
 *
 * <h3>Pricing model</h3>
 * All prices are <b>tax-inclusive</b> (CGST 20% + SGST 20% = 40% total).
 * <pre>
 *   Taxable amount  = price / 1.4    (rounded half-up to 2dp)
 *   Tax amount      = price - taxable
 * </pre>
 *
 * <h3>Glass bottle</h3>
 * 1 crate = 24 bottles. Allowed ₹/bottle: 20, 21, 22, 23, 24, 25.
 *
 * <h3>PET bottle</h3>
 * 1 case = 30 bottles. Allowed ₹/bottle: 22, 23, 24, 25.
 */
@Service
@Slf4j
public class PricingService {

    // ── Constants ──────────────────────────────────────────────────────────

    private static final int GLASS_BOTTLES_PER_UNIT = 24;
    private static final int PET_BOTTLES_PER_UNIT   = 30;

    /** GST divisor: price / 1.4 gives taxable amount (40% inclusive rate). */
    private static final BigDecimal GST_DIVISOR = new BigDecimal("1.4");

    /** CGST and SGST are each 20% of taxable (half of 40% total). */
    private static final BigDecimal CGST_RATE = new BigDecimal("0.20");
    private static final BigDecimal SGST_RATE = new BigDecimal("0.20");

    /** Allowed bottle prices for GLASS type (₹/bottle). */
    private static final Set<Integer> GLASS_ALLOWED_PRICES = Set.of(20, 21, 22, 23, 24, 25);

    /** Allowed bottle prices for PET type (₹/bottle). */
    private static final Set<Integer> PET_ALLOWED_PRICES   = Set.of(22, 23, 24, 25);

    /** Catalogue: bottleType → (pricePerBottle → ratePerUnit) */
    private static final Map<BottleType, Map<Integer, BigDecimal>> CATALOGUE = Map.of(
            BottleType.GLASS, Map.of(
                    20, new BigDecimal("480"),
                    21, new BigDecimal("504"),
                    22, new BigDecimal("528"),
                    23, new BigDecimal("552"),
                    24, new BigDecimal("576"),
                    25, new BigDecimal("600")
            ),
            BottleType.PET, Map.of(
                    22, new BigDecimal("660"),
                    23, new BigDecimal("690"),
                    24, new BigDecimal("720"),
                    25, new BigDecimal("750")
            )
    );

    // ── Public catalogue validation ────────────────────────────────────────

    /**
     * Validate that a price per bottle is allowed for the given bottle type
     * (used for public/homepage orders where prices are fixed).
     *
     * @param bottleType     GLASS or PET
     * @param pricePerBottle the requested price per bottle
     * @throws InvalidPricingException if the price is not in the allowed catalogue
     */
    public void validateCataloguePrice(BottleType bottleType, int pricePerBottle) {
        Set<Integer> allowed = bottleType == BottleType.GLASS
                ? GLASS_ALLOWED_PRICES
                : PET_ALLOWED_PRICES;

        if (!allowed.contains(pricePerBottle)) {
            throw new InvalidPricingException(
                    String.format("Price ₹%d/bottle is not valid for %s bottles. Allowed: %s",
                            pricePerBottle, bottleType, allowed));
        }
    }

    /**
     * Calculate and populate all monetary fields on an {@link OrderItem}.
     * This method is the single source of truth for pricing — frontend totals
     * are completely ignored.
     *
     * @param item the order item with bottleType, pricePerBottle, quantity and flavor set
     * @return the same item with all calculated monetary fields populated
     */
    public OrderItem calculateItemPricing(OrderItem item) {
        BottleType bottleType    = item.getBottleType();
        int        pricePerBottle = item.getPricePerBottle();
        int        quantity       = item.getQuantity();

        // ── Units ──────────────────────────────────────────────────────────
        int bottlesPerUnit = bottleType == BottleType.GLASS
                ? GLASS_BOTTLES_PER_UNIT
                : PET_BOTTLES_PER_UNIT;
        item.setBottlesPerUnit(bottlesPerUnit);

        // ── Rate per unit (tax-inclusive) ──────────────────────────────────
        BigDecimal ratePerUnit = BigDecimal.valueOf((long) pricePerBottle * bottlesPerUnit);
        item.setRatePerUnit(ratePerUnit);

        // ── Taxable per unit: rate / 1.4 ──────────────────────────────────
        BigDecimal taxablePerUnit = ratePerUnit
                .divide(GST_DIVISOR, 2, RoundingMode.HALF_UP);

        // ── Tax per unit: rate - taxable ──────────────────────────────────
        BigDecimal taxPerUnit = ratePerUnit.subtract(taxablePerUnit)
                .setScale(2, RoundingMode.HALF_UP);
        item.setTaxPerUnit(taxPerUnit);

        // total_per_unit == rate_per_unit (price is inclusive)
        item.setTotalPerUnit(ratePerUnit);

        // ── Subtotals ──────────────────────────────────────────────────────
        BigDecimal taxableSubtotal = taxablePerUnit
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
        item.setTaxableSubtotal(taxableSubtotal);

        BigDecimal taxSubtotal = taxPerUnit
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
        item.setTaxSubtotal(taxSubtotal);

        BigDecimal lineTotal = ratePerUnit
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
        item.setLineTotal(lineTotal);

        log.debug("Priced item: type={} ppb={} qty={} → lineTotal={}",
                bottleType, pricePerBottle, quantity, lineTotal);

        return item;
    }

    /**
     * Calculate CGST component from a taxable amount.
     *
     * @param taxableAmount total taxable amount across all items
     * @return CGST (20% of taxable)
     */
    public BigDecimal calculateCgst(BigDecimal taxableAmount) {
        return taxableAmount.multiply(CGST_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate SGST component from a taxable amount.
     *
     * @param taxableAmount total taxable amount across all items
     * @return SGST (20% of taxable)
     */
    public BigDecimal calculateSgst(BigDecimal taxableAmount) {
        return taxableAmount.multiply(SGST_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    // ── Pricing catalogue response ─────────────────────────────────────────

    /**
     * Build the full pricing table response for the public {@code GET /api/public/pricing} endpoint.
     *
     * @return PricingResponse with all glass and PET price points
     */
    public PricingResponse getPricingResponse() {
        List<PricingResponse.GlassPricePoint> glassPrices = GLASS_ALLOWED_PRICES.stream()
                .sorted()
                .map(ppb -> PricingResponse.GlassPricePoint.builder()
                        .ppb(ppb)
                        .crateTotal(new BigDecimal(ppb * GLASS_BOTTLES_PER_UNIT))
                        .build())
                .toList();

        List<PricingResponse.PetPricePoint> petPrices = PET_ALLOWED_PRICES.stream()
                .sorted()
                .map(ppb -> PricingResponse.PetPricePoint.builder()
                        .ppb(ppb)
                        .caseTotal(new BigDecimal(ppb * PET_BOTTLES_PER_UNIT))
                        .build())
                .toList();

        return PricingResponse.builder()
                .glass(PricingResponse.GlassPricing.builder()
                        .bottlesPerCrate(GLASS_BOTTLES_PER_UNIT)
                        .prices(glassPrices)
                        .build())
                .pet(PricingResponse.PetPricing.builder()
                        .bottlesPerCase(PET_BOTTLES_PER_UNIT)
                        .prices(petPrices)
                        .build())
                .build();
    }

    /**
     * Convert string to BottleType enum, throwing a clear error for invalid values.
     *
     * @param type string like "GLASS" or "PET"
     * @return BottleType enum value
     * @throws IllegalArgumentException if string is not a valid type
     */
    public BottleType parseBottleType(String type) {
        try {
            return BottleType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid bottle type: '" + type + "'. Must be GLASS or PET.");
        }
    }
}
