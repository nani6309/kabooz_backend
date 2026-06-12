package com.kabooz.backend.controller;

import com.kabooz.backend.dto.request.CreateDistributorRequest;
import com.kabooz.backend.dto.response.DistributorResponse;
import com.kabooz.backend.service.DistributorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST controller for distributor enquiries.
 * No authentication required.
 *
 * <ul>
 *   <li>POST /api/public/distributors — submit a new distributor enquiry</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/public/distributors")
@RequiredArgsConstructor
@Slf4j
public class PublicDistributorController {

    private final DistributorService distributorService;

    /**
     * Create a new distributor enquiry from the public site.
     *
     * @param req validated create request
     * @return 201 Created with the saved distributor row
     */
    @PostMapping
    public ResponseEntity<DistributorResponse> create(
            @Valid @RequestBody CreateDistributorRequest req) {
        log.info("Public distributor enquiry: mobile={} shopName={}",
                req.getMobile(), req.getShopName());
        DistributorResponse saved = distributorService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}

