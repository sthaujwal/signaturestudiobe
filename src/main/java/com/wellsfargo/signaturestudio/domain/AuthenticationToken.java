package com.wellsfargo.signaturestudio.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Unified authentication token entity.
 * Handles both short-lived authorization codes and long-lived access tokens.
 *
 * Token Types:
 * - AUTHORIZATION_CODE: Short-lived (60s), one-time use, deleted after exchange
 * - ACCESS_TOKEN: Long-lived (30min), reusable, auto-extends on activity
 *
 * CLOB Storage:
 * The auth_obj column stores the authenticated session ID as a plain string.
 * No JSON parsing needed - just the session ID.
 *
 * Flow:
 * 1. After Ping IdP auth → Generate AUTHORIZATION_CODE → Redirect to frontend
 * 2. Frontend exchanges code → Delete code, Generate ACCESS_TOKEN
 * 3. Frontend uses ACCESS_TOKEN for all API calls → Auto-extends on each request
 */
@Entity
@Table(name = "AUTHENTICATION_TOKEN", indexes = {
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

    /**
     * CLOB field containing authenticated session ID as plain string.
     * No JSON - just stores: "session-uuid-1234"
     * This represents which session this token belongs to.
     */
    @Lob
    @Column(name = "auth_obj", nullable = false)
    private String authObj;  // Stores: sessionId as plain string

    @Column(name = "sys_id", nullable = false, length = 255)
    private String sysId;  // Session ID reference (denormalized for indexing)

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
     * Check if token is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(nextExpirTmstp);
    }

    /**
     * Extend token expiration (for ACCESS_TOKEN auto-refresh).
     */
    public void extendExpiration(int validityMinutes) {
        if (tokenType == TokenType.ACCESS_TOKEN) {
            this.nextExpirTmstp = Instant.now().plusSeconds(validityMinutes * 60L);
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
         * Deleted immediately after exchange.
         */
        AUTHORIZATION_CODE,

        /**
         * Long-lived access token (30 minutes, reusable, auto-extends).
         * Used in X-SignatureStudio-Token header for API calls.
         */
        ACCESS_TOKEN
    }
}
