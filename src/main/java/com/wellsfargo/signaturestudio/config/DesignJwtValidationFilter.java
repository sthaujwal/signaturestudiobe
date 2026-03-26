package com.wellsfargo.signaturestudio.config;

import com.wellsfargo.signaturestudio.domain.DesignJwtClaims;
import com.wellsfargo.signaturestudio.service.DesignJwtVerifier;
import com.wellsfargo.signaturestudio.service.DesignTokenReplayService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class DesignJwtValidationFilter extends OncePerRequestFilter {

    public static final String DESIGN_CLAIMS_REQUEST_ATTRIBUTE = "DESIGN_JWT_CLAIMS";
    private static final Logger logger = LoggerFactory.getLogger(DesignJwtValidationFilter.class);

    private final DesignJwtVerifier designJwtVerifier;
    private final DesignTokenReplayService designTokenReplayService;

    public DesignJwtValidationFilter(DesignJwtVerifier designJwtVerifier,
                                     DesignTokenReplayService designTokenReplayService) {
        this.designJwtVerifier = designJwtVerifier;
        this.designTokenReplayService = designTokenReplayService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/api/design/bootstrap".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "Missing bearer token");
            return;
        }

        String token = authHeader.substring(7);
        DesignJwtVerifier.VerifiedDesignJwt verifiedJwt;
        try {
            verifiedJwt = designJwtVerifier.verify(token);
        } catch (Exception ex) {
            logger.warn("Design JWT validation failed", ex);
            writeUnauthorized(response, "Invalid or expired design token");
            return;
        }

        DesignJwtClaims claims = verifiedJwt.claims();
        boolean firstUse = designTokenReplayService.consume(claims.getJti(), verifiedJwt.expiresAt());
        if (!firstUse) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Design token already used\"}");
            return;
        }

        request.setAttribute(DESIGN_CLAIMS_REQUEST_ATTRIBUTE, claims);
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
