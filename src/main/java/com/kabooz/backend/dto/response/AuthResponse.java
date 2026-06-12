package com.kabooz.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after a successful admin login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    /** Token validity in seconds (86400 = 24 hours). */
    private long expiresIn;

    private String username;

    /** Frontend should enforce idle-logout at this many seconds (600 = 10 min). */
    private int idleTimeoutSeconds;
}
