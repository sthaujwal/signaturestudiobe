-- ============================================================================
-- Migration: Add Distributed System Performance Indexes
-- Description: Optimize authentication tokens table for multi-DC deployment
-- Version: V002
-- Author: System
-- Date: 2025-01-02
-- ============================================================================

-- SIMPLIFIED DESIGN: Authorization codes are now deleted immediately after use
-- No need for "used_at" tracking or complex composite indexes

-- Composite index for primary key + expiration (optimal for validateAndExtendAccessToken)
-- Speeds up: SELECT * WHERE authentication_token_id = ? AND next_expir_tmstp > ?
CREATE INDEX idx_id_expiry ON AUTHENTICATION_TOKEN(authentication_token_id, next_expir_tmstp);

-- Composite index for primary key + token type + expiration (optimal for token validation)
-- Speeds up: SELECT * WHERE authentication_token_id = ? AND token_type = ? AND next_expir_tmstp > ?
CREATE INDEX idx_id_type_expiry ON AUTHENTICATION_TOKEN(authentication_token_id, token_type, next_expir_tmstp);

-- Composite index for session-based queries with type filter
-- Speeds up: SELECT * WHERE sys_id = ? AND token_type = ?
CREATE INDEX idx_sys_id_type ON AUTHENTICATION_TOKEN(sys_id, token_type);

-- Add comments for indexes
COMMENT ON INDEX idx_id_expiry IS 'Composite index for token validation with expiration check (supports all token types)';
COMMENT ON INDEX idx_id_type_expiry IS 'Composite index for token validation with type and expiration check (distributed system optimization)';
COMMENT ON INDEX idx_sys_id_type IS 'Composite index for session-based token queries with type filter';

-- Gather statistics for optimizer
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS(
        ownname => USER,
        tabname => 'AUTHENTICATION_TOKEN',
        estimate_percent => DBMS_STATS.AUTO_SAMPLE_SIZE,
        method_opt => 'FOR ALL COLUMNS SIZE AUTO',
        cascade => TRUE
    );
END;
/
