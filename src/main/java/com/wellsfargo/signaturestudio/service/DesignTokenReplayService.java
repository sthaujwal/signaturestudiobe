package com.wellsfargo.signaturestudio.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory replay protection for design JWT jti claim.
 * NOTE: For multi-instance deployments, replace with shared storage (DB/Redis).
 */
@Service
public class DesignTokenReplayService {

    private final Map<String, Instant> consumedJti = new ConcurrentHashMap<>();

    public boolean consume(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return false;
        }

        cleanupExpiredEntries();
        Instant existing = consumedJti.putIfAbsent(jti, expiresAt);
        return existing == null;
    }

    private void cleanupExpiredEntries() {
        Instant now = Instant.now();
        consumedJti.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
