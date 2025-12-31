package com.wellsfargo.signaturestudio.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Unified authentication token entity.
 * Handles both short-lived authorization codes and long-lived access tokens.
 *
 * Token Types:
 * - AUTHORIZATION_CODE: Short-lived (60s), one-time use, exchanged for ACCESS_TOKEN
 * - ACCESS_TOKEN: Long-lived (30min), reusable, auto-extends on activity
 *
 * CLOB Storage:
 * The auth_obj column is a CLOB field containing JSON metadata:
 * {
 *   "tokenValue": "abc123xyz...",
 *   "used": false,
 *   "usedAt": null,
 *   "lastUsedAt": null
 * }
 *
 * Flow:
 * 1. After Ping IdP auth → Generate AUTHORIZATION_CODE → Redirect to frontend
 * 2. Frontend exchanges code → Generate ACCESS_TOKEN
 * 3. Frontend uses ACCESS_TOKEN for all API calls → Auto-extends on each request
 */
@Entity
@Table(name = "AUTHENTICATION_TOKENS", indexes = {
    @Index(name = "idx_sys_id", columnList = "sys_id"),
    @Index(name = "idx_token_type", columnList = "token_type"),
    @Index(name = "idx_expiration", columnList = "next_expir_tmstp")
})
public class AuthenticationToken {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationToken.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Id
    @Column(name = "authentication_token_id", length = 64)
    private String authenticationTokenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 20)
    private TokenType tokenType;

    /**
     * CLOB field containing JSON metadata.
     * Structure: { "tokenValue": "...", "used": false, "usedAt": null, "lastUsedAt": null }
     */
    @Lob
    @Column(name = "auth_obj", nullable = false)
    private String authObj;

    /**
     * Transient field to hold parsed metadata from authObj CLOB.
     * Not stored in database - derived from authObj JSON.
     */
    @Transient
    private TokenMetadata metadata;

    @Column(name = "sys_id", nullable = false, length = 255)
    private String sysId;  // Session ID reference

    @Column(name = "expir_prod_in_min", nullable = false)
    private Integer expirProdInMin;

    @Column(name = "next_expir_tmstp", nullable = false)
    private Instant nextExpirTmstp;

    @Column(name = "row_crte_tmstp", nullable = false, updatable = false)
    private Instant rowCrteTmstp;

    @Column(name = "row_lst_updt_tmstp", nullable = false)
    private Instant rowLstUpdtTmstp;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        rowCrteTmstp = now;
        rowLstUpdtTmstp = now;
    }

    @PreUpdate
    protected void onUpdate() {
        rowLstUpdtTmstp = Instant.now();
    }

    /**
     * Parse JSON from authObj CLOB after loading from database.
     */
    @PostLoad
    protected void parseMetadata() {
        if (authObj != null) {
            try {
                this.metadata = objectMapper.readValue(authObj, TokenMetadata.class);
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse token metadata from authObj CLOB", e);
                this.metadata = new TokenMetadata();
            }
        }
    }

    /**
     * Get token value from metadata.
     */
    public String getTokenValue() {
        ensureMetadataLoaded();
        return metadata != null ? metadata.tokenValue : null;
    }

    /**
     * Set token value in metadata and update CLOB.
     */
    public void setTokenValue(String tokenValue) {
        ensureMetadataLoaded();
        metadata.tokenValue = tokenValue;
        updateAuthObjFromMetadata();
    }

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(nextExpirTmstp);
    }

    /**
     * Check if authorization code has been used (one-time use check).
     */
    public boolean isUsed() {
        ensureMetadataLoaded();
        return tokenType == TokenType.AUTHORIZATION_CODE && metadata != null && metadata.used;
    }

    /**
     * Mark authorization code as consumed.
     */
    public void markAsUsed() {
        if (tokenType == TokenType.AUTHORIZATION_CODE) {
            ensureMetadataLoaded();
            metadata.used = true;
            metadata.usedAt = Instant.now();
            updateAuthObjFromMetadata();
        }
    }

    /**
     * Extend token expiration (for ACCESS_TOKEN auto-refresh).
     */
    public void extendExpiration(int validityMinutes) {
        if (tokenType == TokenType.ACCESS_TOKEN) {
            this.nextExpirTmstp = Instant.now().plusSeconds(validityMinutes * 60L);
            ensureMetadataLoaded();
            metadata.lastUsedAt = Instant.now();
            updateAuthObjFromMetadata();
        }
    }

    /**
     * Ensure metadata is loaded from authObj CLOB.
     */
    private void ensureMetadataLoaded() {
        if (metadata == null) {
            if (authObj != null) {
                parseMetadata();
            } else {
                metadata = new TokenMetadata();
            }
        }
    }

    /**
     * Update authObj CLOB from metadata object.
     */
    private void updateAuthObjFromMetadata() {
        try {
            this.authObj = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize token metadata to JSON", e);
        }
    }

    // Getters and setters
    public String getAuthenticationTokenId() {
        return authenticationTokenId;
    }

    public void setAuthenticationTokenId(String authenticationTokenId) {
        this.authenticationTokenId = authenticationTokenId;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getAuthObj() {
        return authObj;
    }

    public void setAuthObj(String authObj) {
        this.authObj = authObj;
    }

    public String getSysId() {
        return sysId;
    }

    public void setSysId(String sysId) {
        this.sysId = sysId;
    }

    public Integer getExpirProdInMin() {
        return expirProdInMin;
    }

    public void setExpirProdInMin(Integer expirProdInMin) {
        this.expirProdInMin = expirProdInMin;
    }

    public Instant getNextExpirTmstp() {
        return nextExpirTmstp;
    }

    public void setNextExpirTmstp(Instant nextExpirTmstp) {
        this.nextExpirTmstp = nextExpirTmstp;
    }

    public Instant getRowCrteTmstp() {
        return rowCrteTmstp;
    }

    public void setRowCrteTmstp(Instant rowCrteTmstp) {
        this.rowCrteTmstp = rowCrteTmstp;
    }

    public Instant getRowLstUpdtTmstp() {
        return rowLstUpdtTmstp;
    }

    public void setRowLstUpdtTmstp(Instant rowLstUpdtTmstp) {
        this.rowLstUpdtTmstp = rowLstUpdtTmstp;
    }

    /**
     * Token type enumeration.
     */
    public enum TokenType {
        /**
         * Short-lived authorization code (60 seconds, one-time use).
         * Used in redirect URL, exchanged for ACCESS_TOKEN.
         */
        AUTHORIZATION_CODE,

        /**
         * Long-lived access token (30 minutes, reusable, auto-extends).
         * Used in X-SignatureStudio-Token header for API calls.
         */
        ACCESS_TOKEN
    }

    /**
     * JSON metadata stored in auth_obj CLOB.
     * Stores token value and usage tracking information.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenMetadata {
        /**
         * The actual token value (random Base64 string).
         */
        public String tokenValue;

        /**
         * Whether authorization code has been used (one-time use tracking).
         * Only applicable for AUTHORIZATION_CODE type.
         */
        public boolean used = false;

        /**
         * Timestamp when authorization code was consumed.
         * Only applicable for AUTHORIZATION_CODE type.
         */
        public Instant usedAt;

        /**
         * Last time this access token was used in an API request.
         * Only applicable for ACCESS_TOKEN type.
         */
        public Instant lastUsedAt;

        public TokenMetadata() {
        }

        public TokenMetadata(String tokenValue) {
            this.tokenValue = tokenValue;
        }
    }
}
