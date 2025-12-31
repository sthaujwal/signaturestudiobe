package com.wellsfargo.signaturestudio.config;

import com.wellsfargo.signaturestudio.service.AuthenticationTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Custom authentication filter that validates access tokens from X-SignatureStudio-Token header.
 *
 * Flow:
 * 1. Extract token from X-SignatureStudio-Token header
 * 2. Validate and extend token expiration (auto-refresh)
 * 3. Load session from Oracle
 * 4. Touch session to extend session expiration
 * 5. Make session available to downstream filters/controllers
 *
 * Security features:
 * - Token validation on every request
 * - Automatic token extension (sliding expiration)
 * - Session synchronization (token and session expire together)
 * - Returns 401 for invalid/expired tokens
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticationFilter.class);
    private static final String TOKEN_HEADER = "X-SignatureStudio-Token";

    private final AuthenticationTokenService tokenService;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public TokenAuthenticationFilter(
            AuthenticationTokenService tokenService,
            @Nullable FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String tokenValue = request.getHeader(TOKEN_HEADER);

        if (tokenValue != null && !tokenValue.isEmpty()) {
            // Validate and extend ACCESS_TOKEN (automatic extension!)
            Optional<String> sessionIdOpt = tokenService.validateAndExtendAccessToken(tokenValue);

            if (sessionIdOpt.isPresent()) {
                String sessionId = sessionIdOpt.get();

                // Load and touch session (extends session expiration)
                if (sessionRepository != null) {
                    Session session = sessionRepository.findById(sessionId);

                    if (session != null && !session.isExpired()) {
                        // CRITICAL: Touch session to extend its expiration
                        // This keeps token and session expiration synchronized
                        session.setLastAccessedTime(Instant.now());
                        // Safe cast since we know the session type matches the repository
                        @SuppressWarnings("unchecked")
                        FindByIndexNameSessionRepository<Session> repo =
                            (FindByIndexNameSessionRepository<Session>) sessionRepository;
                        repo.save(session);

                        // Make session ID available to controllers
                        request.setAttribute("AUTHENTICATED_SESSION_ID", sessionId);

                        logger.debug("Token validated and session extended: {}", sessionId);

                        // Continue filter chain
                        filterChain.doFilter(request, response);
                        return;
                    } else {
                        logger.warn("Token valid but session not found or expired: {}", sessionId);
                    }
                } else {
                    logger.warn("Session repository not available, cannot validate session");
                }
            } else {
                logger.debug("Invalid or expired access token");
            }
        } else {
            logger.debug("No token provided in {} header", TOKEN_HEADER);
        }

        // No valid token - return 401 Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Don't filter public endpoints and auth endpoints
        String path = request.getRequestURI();
        return path.startsWith("/api/public/") ||
               path.equals("/api/auth/login") ||
               path.equals("/api/auth/callback") ||
               path.equals("/api/auth/exchange");  // Allow token exchange without token
    }
}
