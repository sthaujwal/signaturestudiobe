-- ============================================================================
-- Migration: Create Authentication Tokens Table
-- Description: Unified table for managing both authorization codes and access tokens
-- Version: V001
-- Author: System
-- Date: 2025-01-01
-- ============================================================================

-- Create the authentication tokens table
-- Note: Table name matches JPA entity @Table annotation (singular: AUTHENTICATION_TOKEN)
CREATE TABLE AUTHENTICATION_TOKEN (
    -- Primary key (UUID used as token value for optimal lookups)
    authentication_token_id VARCHAR2(64) NOT NULL,

    -- Token type: 'AUTHORIZATION_CODE' or 'ACCESS_TOKEN'
    token_type VARCHAR2(20) NOT NULL,

    -- CLOB containing authenticated session ID as plain string (no JSON)
    -- Stores: sessionId (e.g., "session-uuid-1234")
    auth_obj CLOB NOT NULL,

    -- Reference to Spring Session (denormalized for indexing)
    sys_id VARCHAR2(255) NOT NULL,

    -- Expiration configuration (in minutes)
    expir_prod_in_min NUMBER(10) NOT NULL,

    -- Next expiration timestamp (gets extended on use for ACCESS_TOKEN)
    -- IMPORTANT: Use TIMESTAMP (not TIMESTAMP WITH TIME ZONE)
    -- Hibernate stores Instant as TIMESTAMP and handles UTC conversion
    next_expir_tmstp TIMESTAMP NOT NULL,

    -- Audit timestamps
    row_crte_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    row_lst_updt_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT pk_authentication_token PRIMARY KEY (authentication_token_id),
    CONSTRAINT chk_token_type CHECK (token_type IN ('AUTHORIZATION_CODE', 'ACCESS_TOKEN')),
    CONSTRAINT fk_auth_token_session FOREIGN KEY (sys_id)
        REFERENCES SPRING_SESSION(SESSION_ID) ON DELETE CASCADE
);

-- Create indexes for performance
-- Note: These match the @Index annotations in the JPA entity
CREATE INDEX idx_sys_id ON AUTHENTICATION_TOKEN(sys_id);
CREATE INDEX idx_token_type ON AUTHENTICATION_TOKEN(token_type);
CREATE INDEX idx_expiration ON AUTHENTICATION_TOKEN(next_expir_tmstp);

-- Create trigger for automatic row_lst_updt_tmstp update
-- Note: JPA @PreUpdate also sets this, but trigger ensures consistency
CREATE OR REPLACE TRIGGER trg_auth_token_update
BEFORE UPDATE ON AUTHENTICATION_TOKEN
FOR EACH ROW
BEGIN
    :NEW.row_lst_updt_tmstp := CURRENT_TIMESTAMP;
END;
/

-- Add comments for documentation
COMMENT ON TABLE AUTHENTICATION_TOKEN IS 'Unified table for managing authentication tokens (authorization codes and access tokens)';
COMMENT ON COLUMN AUTHENTICATION_TOKEN.authentication_token_id IS 'Primary key (UUID - used as token value for optimal lookups)';
COMMENT ON COLUMN AUTHENTICATION_TOKEN.token_type IS 'Type of token: AUTHORIZATION_CODE (short-lived, one-time, deleted after use) or ACCESS_TOKEN (long-lived, reusable, auto-extends)';
COMMENT ON COLUMN AUTHENTICATION_TOKEN.auth_obj IS 'CLOB containing authenticated session ID as plain string (no JSON)';
COMMENT ON COLUMN AUTHENTICATION_TOKEN.sys_id IS 'Session ID reference (denormalized for indexing, links to SPRING_SESSION table)';
COMMENT ON COLUMN AUTHENTICATION_TOKEN.expir_prod_in_min IS 'Token validity period in minutes (1 for auth codes, 30 for access tokens)';
COMMENT ON COLUMN AUTHENTICATION_TOKEN.next_expir_tmstp IS 'Expiration timestamp in UTC (extended automatically for ACCESS_TOKEN on each use)';
COMMENT ON COLUMN AUTHENTICATION_TOKEN.row_crte_tmstp IS 'Row creation timestamp';
COMMENT ON COLUMN AUTHENTICATION_TOKEN.row_lst_updt_tmstp IS 'Row last update timestamp (auto-updated by trigger and JPA @PreUpdate)';
