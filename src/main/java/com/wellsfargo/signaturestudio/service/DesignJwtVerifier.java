package com.wellsfargo.signaturestudio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellsfargo.signaturestudio.domain.DesignJwtClaims;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class DesignJwtVerifier {

    private final String issuer;
    private final String audience;
    private final String requiredScope;
    private final ObjectMapper objectMapper;
    private final RSAPublicKey publicKey;

    public DesignJwtVerifier(@Value("${design.jwt.issuer}") String issuer,
                             @Value("${design.jwt.audience}") String audience,
                             @Value("${design.jwt.scope:design:view}") String requiredScope,
                             ObjectMapper objectMapper,
                             @Value("${design.jwt.public-key}") String pemPublicKey) {
        this.issuer = issuer;
        this.audience = audience;
        this.requiredScope = requiredScope;
        this.objectMapper = objectMapper;
        this.publicKey = parsePublicKey(pemPublicKey);
    }

    public VerifiedDesignJwt verify(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                throw new ServiceException(ErrorCode.UNAUTHORIZED, "Malformed design token");
            }

            if (!verifySignature(parts[0], parts[1], parts[2])) {
                throw new ServiceException(ErrorCode.UNAUTHORIZED, "Invalid design token signature");
            }

            Map<String, Object> claims = parseClaims(parts[1]);
            validateClaims(claims);

            DesignJwtClaims designClaims = new DesignJwtClaims(
                stringClaim(claims, "jti"),
                stringClaim(claims, "transactionId"),
                stringClaim(claims, "accountId"),
                stringClaim(claims, "sub")
            );

            return new VerifiedDesignJwt(
                designClaims,
                Instant.ofEpochSecond(longClaim(claims, "exp"))
            );
        } catch (ParseException e) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Malformed design token", e);
        } catch (Exception e) {
            if (e instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Unable to verify design token", e);
        }
    }

    private void validateClaims(Map<String, Object> claims) {
        Instant now = Instant.now();
        long clockSkewSeconds = Duration.ofSeconds(30).getSeconds();

        if (!issuer.equals(stringClaim(claims, "iss"))) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Invalid design token issuer");
        }

        if (!audienceMatches(claims.get("aud"))) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Invalid design token audience");
        }

        long exp = longClaim(claims, "exp");
        if (now.isAfter(Instant.ofEpochSecond(exp + clockSkewSeconds))) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Design token expired");
        }

        if (claims.get("nbf") != null) {
            long nbf = longClaim(claims, "nbf");
            if (now.isBefore(Instant.ofEpochSecond(nbf - clockSkewSeconds))) {
                throw new ServiceException(ErrorCode.UNAUTHORIZED, "Design token not active yet");
            }
        }

        if (claims.get("iat") != null) {
            long iat = longClaim(claims, "iat");
            if (Instant.ofEpochSecond(iat).isAfter(now.plusSeconds(clockSkewSeconds))) {
                throw new ServiceException(ErrorCode.UNAUTHORIZED, "Design token issued in the future");
            }
        }

        String jti = stringClaim(claims, "jti");
        String transactionId = stringClaim(claims, "transactionId");
        String scope = stringClaim(claims, "scope");

        if (jti.isBlank()) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Design token missing jti");
        }
        if (transactionId.isBlank()) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Design token missing transactionId");
        }
        if (scope.isBlank() || !scope.contains(requiredScope)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Design token missing required scope");
        }
    }

    private boolean verifySignature(String encodedHeader, String encodedPayload, String encodedSignature) {
        try {
            String signingInput = encodedHeader + "." + encodedPayload;
            byte[] signatureBytes = Base64.getUrlDecoder().decode(encodedSignature);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(signingInput.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Unable to verify token signature", e);
        }
    }

    private Map<String, Object> parseClaims(String encodedPayload) throws ParseException {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ParseException("Unable to parse JWT payload", 0);
        }
    }

    private boolean audienceMatches(Object audClaim) {
        if (audClaim instanceof String aud) {
            return audience.equals(aud);
        }
        if (audClaim instanceof List<?> list) {
            return list.stream().anyMatch(audience::equals);
        }
        return false;
    }

    private String stringClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private long longClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Design token missing " + key);
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Invalid numeric claim: " + key);
        }
    }

    private RSAPublicKey parsePublicKey(String pemPublicKey) {
        if (pemPublicKey == null || pemPublicKey.isBlank()) {
            throw new IllegalStateException("design.jwt.public-key must be configured");
        }
        try {
            String normalized = pemPublicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse design JWT public key", e);
        }
    }

    public record VerifiedDesignJwt(DesignJwtClaims claims, Instant expiresAt) {}
}
