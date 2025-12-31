-- ============================================================================
-- Migration: Create Authentication Tokens Table
-- Description: Unified table for managing both authorization codes and access tokens
-- Version: V001
-- Author: System
-- Date: 2025-01-01
-- ============================================================================

-- Create the authentication tokens table
CREATE TABLE AUTHENTICATION_TOKENS (
    -- Primary key
    authentication_token_id VARCHAR2(64) NOT NULL,

    -- Token type: 'AUTHORIZATION_CODE' or 'ACCESS_TOKEN'
    token_type VARCHAR2(20) NOT NULL,

    -- The actual token value (what gets sent in requests)
    auth_obj VARCHAR2(128) NOT NULL,

    -- Reference to Spring Session
    sys_id VARCHAR2(255) NOT NULL,

    -- Expiration configuration (in minutes)
    expir_prod_in_min NUMBER(10) NOT NULL,

    -- Next expiration timestamp (gets extended on use for ACCESS_TOKEN)
    next_expir_tmstp TIMESTAMP NOT NULL,

    -- Usage tracking
    last_used_tmstp TIMESTAMP,

    -- For AUTHORIZATION_CODE: marks as consumed (one-time use)
    used_at TIMESTAMP,

    -- Audit timestamps
    row_crte_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    row_lst_updt_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT pk_authentication_tokens PRIMARY KEY (authentication_token_id),
    CONSTRAINT uk_auth_obj UNIQUE (auth_obj),
    CONSTRAINT chk_token_type CHECK (token_type IN ('AUTHORIZATION_CODE', 'ACCESS_TOKEN')),
    CONSTRAINT fk_auth_token_session FOREIGN KEY (sys_id)
        REFERENCES SPRING_SESSION(SESSION_ID) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE UNIQUE INDEX idx_auth_obj ON AUTHENTICATION_TOKENS(auth_obj);
CREATE INDEX idx_sys_id ON AUTHENTICATION_TOKENS(sys_id);
CREATE INDEX idx_token_type ON AUTHENTICATION_TOKENS(token_type);
CREATE INDEX idx_expiration ON AUTHENTICATION_TOKENS(next_expir_tmstp);

-- Create trigger for automatic row_lst_updt_tmstp update
CREATE OR REPLACE TRIGGER trg_auth_token_update
BEFORE UPDATE ON AUTHENTICATION_TOKENS
FOR EACH ROW
BEGIN
    :NEW.row_lst_updt_tmstp := CURRENT_TIMESTAMP;
END;
/

-- Add comments for documentation
COMMENT ON TABLE AUTHENTICATION_TOKENS IS 'Unified table for managing authentication tokens (authorization codes and access tokens)';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.authentication_token_id IS 'Primary key (UUID)';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.token_type IS 'Type of token: AUTHORIZATION_CODE (short-lived, one-time) or ACCESS_TOKEN (long-lived, reusable)';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.auth_obj IS 'The actual token value sent in requests';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.sys_id IS 'Session ID reference (links to SPRING_SESSION table)';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.expir_prod_in_min IS 'Token validity period in minutes (1 for auth codes, 30 for access tokens)';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.next_expir_tmstp IS 'Expiration timestamp (extended automatically for ACCESS_TOKEN on each use)';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.last_used_tmstp IS 'Last time the token was used (for ACCESS_TOKEN only)';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.used_at IS 'When authorization code was consumed (for AUTHORIZATION_CODE only, prevents replay)';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.row_crte_tmstp IS 'Row creation timestamp';
COMMENT ON COLUMN AUTHENTICATION_TOKENS.row_lst_updt_tmstp IS 'Row last update timestamp (auto-updated by trigger)';
