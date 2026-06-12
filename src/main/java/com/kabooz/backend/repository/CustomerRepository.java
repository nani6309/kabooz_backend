package com.kabooz.backend.repository;

import com.kabooz.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Customer} entities.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find a customer by their unique mobile number (used for upsert logic).
     *
     * @param mobile 10-digit mobile number string
     * @return optional customer
     */
    Optional<Customer> findByMobile(String mobile);
}
