package com.kabooz.backend.service;

import com.kabooz.backend.dto.request.CreateDistributorRequest;
import com.kabooz.backend.dto.response.DistributorResponse;
import com.kabooz.backend.entity.Distributor;
import com.kabooz.backend.exception.DistributorNotFoundException;
import com.kabooz.backend.repository.DistributorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for distributor enquiries (create, list, delete).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributorService {

    private final DistributorRepository distributorRepository;

    /**
     * Persist a new distributor enquiry submitted from the public homepage.
     * Status is forced to {@link Distributor.Status#NEW}.
     *
     * @param req validated create request
     * @return persisted distributor DTO
     */
    @Transactional
    public DistributorResponse create(CreateDistributorRequest req) {
        Distributor entity = Distributor.builder()
                .name(req.getName().trim())
                .mobile(req.getMobile().trim())
                .shopName(req.getShopName().trim())
                .address(req.getAddress().trim())
                .status(Distributor.Status.NEW)
                .build();

        Distributor saved = distributorRepository.save(entity);
        log.info("New distributor enquiry saved: id={} mobile={}", saved.getId(), saved.getMobile());
        return DistributorResponse.from(saved);
    }

    /**
     * Paginated, searchable distributor list (admin).
     *
     * @param page   0-based page number
     * @param size   page size (1..200)
     * @param search optional free-text search term
     * @return Spring {@link Page} of distributor DTOs
     */
    @Transactional(readOnly = true)
    public Page<DistributorResponse> list(int page, int size, String search) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        String term = (search == null || search.isBlank()) ? null : search.trim();
        return distributorRepository.search(term, pageable)
                .map(DistributorResponse::from);
    }

    /**
     * Hard-delete a distributor by id.
     *
     * @param id distributor id
     * @throws DistributorNotFoundException if no record exists
     */
    @Transactional
    public void delete(Long id) {
        if (!distributorRepository.existsById(id)) {
            throw new DistributorNotFoundException(id);
        }
        distributorRepository.deleteById(id);
        log.info("Distributor deleted: id={}", id);
    }
}

