package com.kabooz.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handler that converts all exceptions to a consistent JSON structure:
 * <pre>
 * {
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "...",
 *   "timestamp": "..."
 * }
 * </pre>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 400 — Validation ──────────────────────────────────────────────────

    /**
     * Handles Bean Validation failures from {@code @Valid} annotated request bodies.
     * Aggregates all field errors into a single comma-separated message.
     *
     * @param ex the validation exception
     * @return 400 response with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        // Also expose individual field errors for richer client-side handling
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));

        log.warn("Validation failed: {}", message);

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST.value(), "Validation Failed", message);
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles invalid pricing requests.
     *
     * @param ex the pricing exception
     * @return 400 response
     */
    @ExceptionHandler(InvalidPricingException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPricing(InvalidPricingException ex) {
        log.warn("Invalid pricing: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(buildBody(400, "Invalid Pricing", ex.getMessage()));
    }

    /**
     * Handles general illegal argument errors.
     *
     * @param ex the exception
     * @return 400 response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(buildBody(400, "Bad Request", ex.getMessage()));
    }

    // ── 401 — Unauthorized ────────────────────────────────────────────────

    /**
     * Handles explicit unauthorized exceptions thrown by the application layer.
     *
     * @param ex the unauthorized exception
     * @return 401 response
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildBody(401, "Unauthorized", ex.getMessage()));
    }

    /**
     * Handles Spring Security authentication failures.
     *
     * @param ex the authentication exception
     * @return 401 response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildBody(401, "Unauthorized", "Authentication required"));
    }

    // ── 403 — Forbidden ───────────────────────────────────────────────────

    /**
     * Handles Spring Security access denied (wrong role).
     *
     * @param ex the access denied exception
     * @return 403 response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildBody(403, "Forbidden", "You do not have permission to access this resource"));
    }

    // ── 404 — Not Found ───────────────────────────────────────────────────

    /**
     * Handles order not found exceptions.
     *
     * @param ex the not found exception
     * @return 404 response
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildBody(404, "Not Found", ex.getMessage()));
    }

    /**
     * Handles distributor not found exceptions.
     *
     * @param ex the not found exception
     * @return 404 response
     */
    @ExceptionHandler(DistributorNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDistributorNotFound(DistributorNotFoundException ex) {
        log.warn("Distributor not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildBody(404, "Not Found", ex.getMessage()));
    }

    // ── 409 — Conflict ────────────────────────────────────────────────────

    /**
     * Handles database constraint violations (duplicate invoice no, duplicate mobile, etc.).
     *
     * @param ex the data integrity exception
     * @return 409 response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildBody(409, "Conflict", "A record with the same unique key already exists"));
    }

    // ── 500 — Internal Server Error ───────────────────────────────────────

    /**
     * Catch-all handler for unexpected exceptions.
     * Stack trace is intentionally hidden from the response.
     *
     * @param ex the exception
     * @return 500 response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildBody(500, "Internal Server Error",
                        "An unexpected error occurred. Please contact support."));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Map<String, Object> buildBody(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }
}
