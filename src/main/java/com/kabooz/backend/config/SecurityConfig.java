package com.kabooz.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kabooz.backend.security.JwtAuthenticationFilter;
import com.kabooz.backend.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Security 6 configuration.
 * <ul>
 *   <li>Stateless JWT — no sessions</li>
 *   <li>No CSRF (REST API)</li>
 *   <li>/api/public/** is PERMIT_ALL</li>
 *   <li>/api/admin/** requires ROLE_ADMIN + valid JWT</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    /**
     * Configure the main security filter chain.
     *
     * @param http the HttpSecurity builder
     * @return configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Disable CSRF (stateless REST) ──────────────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS handled by CorsConfig bean ────────────────────────
            .cors(cors -> {}) // picks up CorsConfigurationSource bean

            // ── Session management: STATELESS ──────────────────────────
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Authorisation rules ────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Uptime-monitor / health — no auth needed
                .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/").permitAll()
                .requestMatchers("/api/health").permitAll()
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Admin endpoints require authentication
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Everything else denied
                .anyRequest().denyAll()
            )

            // ── 401 entry point ────────────────────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("status", 401);
                    body.put("error", "Unauthorized");
                    body.put("message", "Authentication required");
                    body.put("timestamp", LocalDateTime.now().toString());
                    objectMapper.writeValue(response.getOutputStream(), body);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("status", 403);
                    body.put("error", "Forbidden");
                    body.put("message", "You do not have permission to access this resource");
                    body.put("timestamp", LocalDateTime.now().toString());
                    objectMapper.writeValue(response.getOutputStream(), body);
                })
            )

            // ── Authentication Provider ───────────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── JWT filter ─────────────────────────────────────────────
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder (cost 10).
     *
     * @return PasswordEncoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * DAO authentication provider wiring UserDetailsService and PasswordEncoder.
     *
     * @return DaoAuthenticationProvider bean
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expose the AuthenticationManager as a bean (used in AuthService).
     *
     * @param authConfig the authentication configuration
     * @return AuthenticationManager bean
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
