package com.kabooz.backend.controller;

import com.kabooz.backend.dto.response.DistributorResponse;
import com.kabooz.backend.service.DistributorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST controller for distributor management.
 * All endpoints require a valid JWT with ROLE_ADMIN.
 *
 * <ul>
 *   <li>GET    /api/admin/distributors        — paginated, searchable list</li>
 *   <li>DELETE /api/admin/distributors/{id}   — delete a distributor</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/distributors")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDistributorController {

    private final DistributorService distributorService;

    /**
     * Paginated list of distributors with optional free-text search.
     *
     * @param page   0-based page index (default 0)
     * @param size   page size 1..200 (default 20)
     * @param search optional search term (name, mobile, shop name, address)
     * @return Spring Page of distributor DTOs
     */
    @GetMapping
    public ResponseEntity<Page<DistributorResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        log.debug("Admin list distributors: page={} size={} search={}", page, size, search);
        return ResponseEntity.ok(distributorService.list(page, size, search));
    }

    /**
     * Delete a distributor by id.
     *
     * @param id distributor id
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Admin deleting distributor id={}", id);
        distributorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

