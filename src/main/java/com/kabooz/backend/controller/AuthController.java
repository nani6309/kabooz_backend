package com.kabooz.backend.controller;

import com.kabooz.backend.dto.request.LoginRequest;
import com.kabooz.backend.dto.response.AuthResponse;
import com.kabooz.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * All endpoints are public (PERMIT_ALL in SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Admin login endpoint.
     * <p>
     * POST /api/auth/login
     * Body: {@code { "username": "admin", "password": "kabooz@2024" }}
     * </p>
     *
     * @param req validated login credentials
     * @return 200 with JWT token and metadata
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        log.info("Login request for username={}", req.getUsername());
        AuthResponse response = authService.login(req);
        return ResponseEntity.ok(response);
    }
}
