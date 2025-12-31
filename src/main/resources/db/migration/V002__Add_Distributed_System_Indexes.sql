-- ============================================================================
-- Migration: Add Distributed System Performance Indexes
-- Description: Optimize authentication tokens table for multi-DC deployment
-- Version: V002
-- Author: System
-- Date: 2025-01-01
-- ============================================================================

-- Add composite index for atomic token extension query
-- Speeds up: UPDATE ... WHERE auth_obj = ? AND token_type = 'ACCESS_TOKEN' AND next_expir_tmstp > SYSTIMESTAMP
CREATE INDEX idx_auth_obj_type_expiry ON AUTHENTICATION_TOKENS(auth_obj, token_type, next_expir_tmstp);

-- Add composite index for atomic authorization code consumption
-- Speeds up: UPDATE ... WHERE auth_obj = ? AND token_type = 'AUTHORIZATION_CODE' AND used_at IS NULL
CREATE INDEX idx_auth_obj_type_used ON AUTHENTICATION_TOKENS(auth_obj, token_type, used_at);

-- Add index for session-based queries (token revocation on logout)
-- Speeds up: DELETE FROM ... WHERE sys_id = ?
-- Note: Already have idx_sys_id from V001, but this is a reminder

-- Add index for cleanup queries
-- Speeds up: DELETE FROM ... WHERE next_expir_tmstp < SYSTIMESTAMP
-- Note: Already have idx_expiration from V001, but this is a reminder

-- Add index for used authorization code cleanup
-- Speeds up: DELETE FROM ... WHERE token_type = 'AUTHORIZATION_CODE' AND used_at IS NOT NULL AND used_at < ...
CREATE INDEX idx_token_type_used_at ON AUTHENTICATION_TOKENS(token_type, used_at);

-- Add comments for new indexes
COMMENT ON INDEX idx_auth_obj_type_expiry IS 'Composite index for atomic access token extension (distributed system optimization)';
COMMENT ON INDEX idx_auth_obj_type_used IS 'Composite index for atomic authorization code consumption (distributed system optimization)';
COMMENT ON INDEX idx_token_type_used_at IS 'Composite index for cleanup of used authorization codes';

-- Gather statistics for optimizer
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS(
        ownname => USER,
        tabname => 'AUTHENTICATION_TOKENS',
        estimate_percent => DBMS_STATS.AUTO_SAMPLE_SIZE,
        method_opt => 'FOR ALL COLUMNS SIZE AUTO',
        cascade => TRUE
    );
END;
/
