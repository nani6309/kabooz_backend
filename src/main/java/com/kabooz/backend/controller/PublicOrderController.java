package com.kabooz.backend.controller;

import com.kabooz.backend.dto.request.PlaceOrderRequest;
import com.kabooz.backend.dto.response.PlaceOrderResponse;
import com.kabooz.backend.dto.response.PricingResponse;
import com.kabooz.backend.service.OrderService;
import com.kabooz.backend.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST controller for the Kabooz homepage.
 * No authentication is required for any endpoint in this controller.
 *
 * <ul>
 *   <li>POST /api/public/orders  — place an order</li>
 *   <li>GET  /api/public/pricing — fetch current pricing table</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class PublicOrderController {

    private final OrderService   orderService;
    private final PricingService pricingService;

    /**
     * Place a new order from the public homepage.
     * Pricing is fully recalculated server-side; frontend totals are ignored.
     *
     * @param req validated order request from the homepage
     * @return 201 Created with invoice number and grand total
     */
    @PostMapping("/orders")
    public ResponseEntity<PlaceOrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest req) {
        log.info("Public order request: customer={} items={}",
                req.getCustomer().getMobile(), req.getItems().size());
        PlaceOrderResponse response = orderService.placePublicOrder(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch the complete pricing catalogue.
     * Used by the homepage to display current bottle prices without hardcoding.
     *
     * @return 200 with glass and PET pricing tables
     */
    @GetMapping("/pricing")
    public ResponseEntity<PricingResponse> getPricing() {
        return ResponseEntity.ok(pricingService.getPricingResponse());
    }
}
