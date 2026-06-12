package com.kabooz.backend.repository;

import com.kabooz.backend.entity.InvoiceCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the {@link InvoiceCounter} singleton row.
 * The pessimistic write lock ensures only one transaction increments at a time.
 */
@Repository
public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounter, Integer> {

    /**
     * Acquire a row-level pessimistic write lock on the counter row (id=1).
     * This is equivalent to {@code SELECT ... FOR UPDATE}.
     *
     * @return optional counter (always present after schema init)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ic FROM InvoiceCounter ic WHERE ic.id = 1")
    Optional<InvoiceCounter> findForUpdate();
}
