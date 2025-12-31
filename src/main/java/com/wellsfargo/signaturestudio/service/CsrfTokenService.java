package com.wellsfargo.signaturestudio.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for managing CSRF tokens programmatically.
 * 
 * This service works with common library Spring Security configurations
 * by accessing CSRF tokens through Spring Security's standard mechanisms.
 * 
 * Usage:
 * - Access token from request attribute (populated by Spring Security filter)
 * - Generate token if not available (using CsrfTokenRepository)
 * - Store token in session for later retrieval
 */
@Service
public class CsrfTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(CsrfTokenService.class);
    
    /**
     * CsrfTokenRepository is injected via constructor if available.
     * Works with common library configurations that expose this bean.
     * If not available (common library doesn't expose it), will use request attribute method only.
     */
    private final CsrfTokenRepository csrfTokenRepository;
    
    /**
     * Constructor injection with optional dependency.
     * Uses @Nullable to indicate this is an optional dependency - Spring will inject null
     * if CsrfTokenRepository is not available (e.g., common library doesn't expose it).
     */
    public CsrfTokenService(@Nullable CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
        logger.debug("CsrfTokenService initialized with repository: {}", 
            csrfTokenRepository != null ? csrfTokenRepository.getClass().getSimpleName() : "null");
    }
    
    /**
     * Gets CSRF token from request attribute (populated by Spring Security filter).
     * This is the preferred method as it uses Spring Security's standard mechanism.
     * 
     * @param request The HTTP request
     * @return Optional containing CsrfToken if available
     */
    public Optional<CsrfToken> getTokenFromRequest(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            logger.debug("Retrieved CSRF token from request attribute");
            return Optional.of(csrfToken);
        }
        return Optional.empty();
    }
    
    /**
     * Generates a new CSRF token using CsrfTokenRepository.
     * This ensures a token is created even if Spring Security filter hasn't run yet.
     * 
     * @param request The HTTP request
     * @param response The HTTP response
     * @return The generated CsrfToken, or null if repository is not available
     */
    public CsrfToken generateToken(HttpServletRequest request, HttpServletResponse response) {
        if (csrfTokenRepository == null) {
            logger.warn("CsrfTokenRepository not available, cannot generate token");
            return null;
        }
        logger.debug("Generating new CSRF token");
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
        logger.debug("CSRF token generated and saved: {}", token.getToken().substring(0, 8) + "...");
        return token;
    }
    
    /**
     * Gets or generates CSRF token.
     * First tries to get from request attribute, if not available, generates a new one.
     * 
     * @param request The HTTP request
     * @param response The HTTP response (required if token needs to be generated)
     * @return The CsrfToken
     */
    public CsrfToken getOrGenerateToken(HttpServletRequest request, HttpServletResponse response) {
        return getTokenFromRequest(request)
            .orElseGet(() -> generateToken(request, response));
    }
    
    /**
     * Gets CSRF token value as a string.
     * Convenience method that returns just the token string.
     * 
     * @param request The HTTP request
     * @param response The HTTP response (required if token needs to be generated)
     * @return The token string, or null if unable to get/generate
     */
    public String getTokenValue(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = getOrGenerateToken(request, response);
        return token != null ? token.getToken() : null;
    }
    
    /**
     * Gets CSRF token header name.
     * Returns the header name that should be used when sending the token.
     * 
     * @param request The HTTP request
     * @param response The HTTP response (required if token needs to be generated)
     * @return The header name (e.g., "X-XSRF-TOKEN")
     */
    public String getHeaderName(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = getOrGenerateToken(request, response);
        return token != null ? token.getHeaderName() : "X-XSRF-TOKEN";
    }
    
    /**
     * Loads CSRF token from session/repository.
     * Useful when you need to retrieve a previously saved token.
     * 
     * @param request The HTTP request
     * @return Optional containing CsrfToken if found in session/repository
     */
    public Optional<CsrfToken> loadToken(HttpServletRequest request) {
        if (csrfTokenRepository == null) {
            return Optional.empty();
        }
        CsrfToken token = csrfTokenRepository.loadToken(request);
        if (token != null) {
            logger.debug("Loaded CSRF token from repository");
            return Optional.of(token);
        }
        return Optional.empty();
    }
}

