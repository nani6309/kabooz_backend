package com.kabooz.backend.repository;

import com.kabooz.backend.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link AdminUser} entities.
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    /**
     * Find an admin by username (case-sensitive match on DB column).
     *
     * @param username the admin username
     * @return optional admin user
     */
    Optional<AdminUser> findByUsername(String username);
}
