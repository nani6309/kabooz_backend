package com.kabooz.backend.service;

import com.kabooz.backend.dto.request.LoginRequest;
import com.kabooz.backend.dto.response.AuthResponse;
import com.kabooz.backend.entity.AdminUser;
import com.kabooz.backend.exception.UnauthorizedException;
import com.kabooz.backend.repository.AdminUserRepository;
import com.kabooz.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service handling admin authentication and brute-force protection.
 *
 * <h3>Brute-Force Policy</h3>
 * After {@code auth.max.failed.attempts} consecutive failed logins, the account is
 * locked for {@code auth.lockout.duration.minutes} minutes. The counters are stored
 * in the {@code admin_users} table so they survive application restarts.
 *
 * <h3>Idle Timeout</h3>
 * The JWT itself is valid for 24 hours. The 10-minute idle timeout is enforced
 * client-side using the {@code idleTimeoutSeconds} hint returned in the auth response.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager  authenticationManager;
    private final JwtTokenProvider       jwtTokenProvider;
    private final AdminUserRepository    adminUserRepository;

    @Value("${auth.max.failed.attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.lockout.duration.minutes:15}")
    private int lockoutDurationMinutes;

    @Value("${admin.idle.timeout.seconds:600}")
    private int idleTimeoutSeconds;

    /**
     * Authenticate an admin user and return a signed JWT.
     * Enforces account lockout after repeated failures.
     *
     * @param req login credentials (username + password)
     * @return AuthResponse containing the JWT, expiry, and idle-timeout hint
     * @throws UnauthorizedException if credentials are wrong or account is locked
     */
    @Transactional
    public AuthResponse login(LoginRequest req) {
        AdminUser adminUser = adminUserRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login attempt for unknown user: {}", req.getUsername());
                    return new UnauthorizedException("Invalid username or password");
                });

        // ── Check account lock ────────────────────────────────────────
        if (isLocked(adminUser)) {
            log.warn("Login attempt on locked account: {}", req.getUsername());
            throw new UnauthorizedException(
                    "Account is temporarily locked due to too many failed attempts. Try again later.");
        }

        // ── Attempt authentication ────────────────────────────────────
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            // Success — reset failure counter
            adminUser.setFailedAttempts(0);
            adminUser.setLockedUntil(null);
            adminUserRepository.save(adminUser);

            String token = jwtTokenProvider.generateToken(authentication);
            long expiresIn = jwtTokenProvider.getExpirationMs() / 1000;

            log.info("Admin '{}' logged in successfully", req.getUsername());

            return AuthResponse.builder()
                    .token(token)
                    .expiresIn(expiresIn)
                    .username(req.getUsername())
                    .idleTimeoutSeconds(idleTimeoutSeconds)
                    .build();

        } catch (BadCredentialsException | LockedException e) {
            // Increment failure counter
            int attempts = adminUser.getFailedAttempts() + 1;
            adminUser.setFailedAttempts(attempts);

            if (attempts >= maxFailedAttempts) {
                adminUser.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
                adminUserRepository.save(adminUser);
                log.warn("Account '{}' locked after {} failed attempts", req.getUsername(), attempts);
                throw new UnauthorizedException(
                        "Account locked after " + maxFailedAttempts +
                        " failed attempts. Try again in " + lockoutDurationMinutes + " minutes.");
            }

            adminUserRepository.save(adminUser);
            log.warn("Failed login for '{}'. Attempt {}/{}", req.getUsername(), attempts, maxFailedAttempts);
            throw new UnauthorizedException("Invalid username or password. " +
                    (maxFailedAttempts - attempts) + " attempt(s) remaining.");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Check if the admin account is currently locked.
     *
     * @param adminUser the admin entity
     * @return true if locked, false if lock has expired or was never set
     */
    private boolean isLocked(AdminUser adminUser) {
        if (adminUser.getLockedUntil() == null) return false;
        if (LocalDateTime.now().isAfter(adminUser.getLockedUntil())) {
            // Lock expired — auto-reset
            adminUser.setFailedAttempts(0);
            adminUser.setLockedUntil(null);
            adminUserRepository.save(adminUser);
            return false;
        }
        return true;
    }
}
