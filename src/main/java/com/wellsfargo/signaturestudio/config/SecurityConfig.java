package com.wellsfargo.signaturestudio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the BFF application.
 *
 * Authentication Strategy:
 * - Custom token-based authentication via X-SignatureStudio-Token header
 * - Token validation handled by TokenAuthenticationFilter
 * - Session management via Spring Session (Oracle JDBC)
 * - CSRF protection disabled (token in header provides CSRF protection)
 *
 * Token Flow:
 * 1. User authenticates via Ping IdP
 * 2. Backend creates session and generates authorization code
 * 3. Frontend exchanges code for access token
 * 4. Frontend uses access token in X-SignatureStudio-Token header
 * 5. TokenAuthenticationFilter validates token and extends expiration
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    public SecurityConfig(TokenAuthenticationFilter tokenAuthenticationFilter) {
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Add custom token authentication filter BEFORE Spring Security's default filters
            // This filter validates X-SignatureStudio-Token header and loads session
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Session management configuration
            .sessionManagement(session -> session
                // STATELESS: Don't create sessions via cookies (tokens are used instead)
                // Sessions are created explicitly after Ping IdP authentication
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false))

            // CSRF protection disabled - token in custom header provides CSRF protection
            // Attackers cannot forge X-SignatureStudio-Token header due to Same-Origin Policy
            .csrf(csrf -> csrf.disable())

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers("/api/public/**").permitAll()

                // Auth endpoints (token not required for these)
                .requestMatchers("/api/auth/login", "/api/auth/callback", "/api/auth/exchange").permitAll()

                // All other endpoints require valid access token
                .anyRequest().authenticated())

            // Disable HTTP Basic auth (not needed with token authentication)
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}


