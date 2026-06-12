package com.kabooz.backend.config;

import com.kabooz.backend.entity.AdminUser;
import com.kabooz.backend.entity.InvoiceCounter;
import com.kabooz.backend.repository.AdminUserRepository;
import com.kabooz.backend.repository.InvoiceCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Automatically seeds the default admin user on startup.
 * Avoids hardcoding password hashes in SQL files by dynamically
 * hashing the configured password using the system's PasswordEncoder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final InvoiceCounterRepository invoiceCounterRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.username:admin}")
    private String defaultUsername;

    @Value("${admin.default.password:kabooz@2024}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedInvoiceCounter();
    }

    private void seedInvoiceCounter() {
        if (invoiceCounterRepository.existsById(1)) {
            log.info("Invoice counter already initialised.");
            return;
        }
        InvoiceCounter counter = new InvoiceCounter();
        counter.setId(1);
        counter.setLastValue(182);
        invoiceCounterRepository.save(counter);
        log.info("Invoice counter seeded with last_value=182.");
    }

    private void seedAdminUser() {
        adminUserRepository.findByUsername(defaultUsername).ifPresentOrElse(
            admin -> {
                log.info("Admin user '{}' already exists. Ensuring password is up to date and unlocking account...", defaultUsername);
                admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
                admin.setFailedAttempts(0);
                admin.setLockedUntil(null);
                adminUserRepository.save(admin);
                log.info("Admin user '{}' successfully updated and unlocked.", defaultUsername);
            },
            () -> {
                log.info("Admin user '{}' not found. Seeding default admin user...", defaultUsername);
                AdminUser admin = AdminUser.builder()
                        .username(defaultUsername)
                        .passwordHash(passwordEncoder.encode(defaultPassword))
                        .failedAttempts(0)
                        .lockedUntil(null)
                        .build();
                adminUserRepository.save(admin);
                log.info("Default admin user '{}' seeded successfully.", defaultUsername);
            }
        );
    }
}
