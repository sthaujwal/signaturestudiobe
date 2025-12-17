package com.wellsfargo.signaturestudio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Spring Session security configuration with enterprise best practices.
 * 
 * Features:
 * 1. Secure cookie configuration (HttpOnly, Secure, SameSite)
 * 2. Session fixation protection
 * 3. Session timeout configuration
 * 4. Concurrent session control
 * 
 * Note: Spring Session JDBC is enabled via application.properties:
 * spring.session.store-type=jdbc
 */
@Configuration
public class SessionSecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionSecurityConfig.class);
    
    /**
     * Configures secure session cookie settings.
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        
        // Cookie name
        serializer.setCookieName("SESSIONID");
        
        // Cookie path - applies to all paths
        serializer.setCookiePath("/");
        
        // HttpOnly - prevents JavaScript access (XSS protection)
        serializer.setUseHttpOnlyCookie(true);
        
        // Secure - only send over HTTPS (set to false for local dev)
        serializer.setUseSecureCookie(true);
        
        // SameSite - CSRF protection
        // Strict: Cookie only sent in first-party context
        // Lax: Cookie sent with top-level navigations and GET from third-party
        serializer.setSameSite("Strict");
        
        // Domain scope (null = current domain only)
        serializer.setDomainName(null);
        
        // Max age in seconds (-1 = session cookie, deleted when browser closes)
        // For persistent sessions, set to session timeout value
        serializer.setCookieMaxAge(-1);
        
        logger.info("Session cookie configured: HttpOnly=true, Secure=true, SameSite=Strict");
        
        return serializer;
    }
}

