package com.wellsfargo.signaturestudio.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Unified authentication token entity.
 * Handles both short-lived authorization codes and long-lived access tokens.
 *
 * Token Types:
 * - AUTHORIZATION_CODE: Short-lived (60s), one-time use, exchanged for ACCESS_TOKEN
 * - ACCESS_TOKEN: Long-lived (30min), reusable, auto-extends on activity
 *
 * Flow:
 * 1. After Ping IdP auth → Generate AUTHORIZATION_CODE → Redirect to frontend
 * 2. Frontend exchanges code → Generate ACCESS_TOKEN
 * 3. Frontend uses ACCESS_TOKEN for all API calls → Auto-extends on each request
 */
@Entity
@Table(name = "AUTHENTICATION_TOKENS", indexes = {
    @Index(name = "idx_auth_obj", columnList = "auth_obj", unique = true),
    @Index(name = "idx_sys_id", columnList = "sys_id"),
    @Index(name = "idx_token_type", columnList = "token_type"),
    @Index(name = "idx_expiration", columnList = "next_expir_tmstp")
})
public class AuthenticationToken {

    @Id
    @Column(name = "authentication_token_id", length = 64)
    private String authenticationTokenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 20)
    private TokenType tokenType;

    @Column(name = "auth_obj", nullable = false, unique = true, length = 128)
    private String authObj;

    @Column(name = "sys_id", nullable = false, length = 255)
    private String sysId;  // Session ID reference

    @Column(name = "expir_prod_in_min", nullable = false)
    private Integer expirProdInMin;

    @Column(name = "next_expir_tmstp", nullable = false)
    private Instant nextExpirTmstp;

    @Column(name = "last_used_tmstp")
    private Instant lastUsedTmstp;

    @Column(name = "used_at")
    private Instant usedAt;  // For AUTHORIZATION_CODE only

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
     * Check if token is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(nextExpirTmstp);
    }

    /**
     * Check if authorization code has been used (one-time use check).
     */
    public boolean isUsed() {
        return tokenType == TokenType.AUTHORIZATION_CODE && usedAt != null;
    }

    /**
     * Mark authorization code as consumed.
     */
    public void markAsUsed() {
        if (tokenType == TokenType.AUTHORIZATION_CODE) {
            this.usedAt = Instant.now();
        }
    }

    /**
     * Extend token expiration (for ACCESS_TOKEN auto-refresh).
     */
    public void extendExpiration() {
        if (tokenType == TokenType.ACCESS_TOKEN) {
            this.nextExpirTmstp = Instant.now().plusSeconds(expirProdInMin * 60L);
            this.lastUsedTmstp = Instant.now();
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

    public Instant getLastUsedTmstp() {
        return lastUsedTmstp;
    }

    public void setLastUsedTmstp(Instant lastUsedTmstp) {
        this.lastUsedTmstp = lastUsedTmstp;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
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
}
