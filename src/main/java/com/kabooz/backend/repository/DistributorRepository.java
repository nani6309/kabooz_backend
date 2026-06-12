package com.kabooz.backend.repository;

import com.kabooz.backend.entity.Distributor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Distributor} entities.
 */
@Repository
public interface DistributorRepository extends JpaRepository<Distributor, Long> {

    /**
     * Paginated distributor list with optional case-insensitive free-text search
     * on name, mobile, shop name, or address.
     *
     * @param search   search term; null or blank returns all
     * @param pageable pagination/sorting parameters
     * @return page of matching distributors
     */
    @Query("""
            SELECT d FROM Distributor d
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(d.name)     LIKE LOWER(CONCAT('%', :search, '%'))
                   OR d.mobile           LIKE CONCAT('%', :search, '%')
                   OR LOWER(d.shopName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(d.address)  LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Distributor> search(@Param("search") String search, Pageable pageable);
}

