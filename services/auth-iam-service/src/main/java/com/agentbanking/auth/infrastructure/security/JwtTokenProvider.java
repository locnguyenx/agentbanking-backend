package com.agentbanking.auth.infrastructure.security;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.port.out.TokenBlacklistRepository;
import com.agentbanking.auth.domain.port.out.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenProvider {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final Key signingKey;
    private final long accessTokenExpiryMinutes;
    private final long refreshTokenExpiryMinutes;

    public JwtTokenProvider(
            TokenBlacklistRepository tokenBlacklistRepository,
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${jwt.access-token-expiration-minutes:15}") long accessTokenExpiryMinutes,
            @Value("${jwt.refresh-token-expiration-minutes:1440}") long refreshTokenExpiryMinutes) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        this.refreshTokenExpiryMinutes = refreshTokenExpiryMinutes;
        
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT secret must be configured and at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    public String generateAccessToken(UserRecord user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("agent_id", user.userId().toString());
        claims.put("permissions", user.permissions());
        claims.put("email", user.email());
        claims.put("fullName", user.fullName());

        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiryMinutes * 60 * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(user.userId().toString())
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(UserRecord user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiryMinutes * 60 * 1000);

        return Jwts.builder()
                .subject(user.userId().toString())
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())
                .signWith(signingKey)
                .compact();
    }

    @Override
    public UUID validateToken(String token) {
        try {
            Claims claims = parseAndVerifyToken(token);
            
            String jti = claims.getId();
            if (tokenBlacklistRepository.findByTokenId(jti).isPresent()) {
                throw new SecurityException("Token has been revoked");
            }

            return UUID.fromString(claims.getSubject());
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Invalid token: " + e.getMessage());
        }
    }

    @Override
    public UUID validateRefreshToken(String refreshToken) {
        try {
            Claims claims = parseAndVerifyToken(refreshToken);

            String jti = claims.getId();
            if (tokenBlacklistRepository.findByTokenId(jti).isPresent()) {
                throw new SecurityException("Refresh token has been revoked");
            }

            return UUID.fromString(claims.getSubject());
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Invalid refresh token: " + e.getMessage());
        }
    }

    @Override
    public Set<String> getPermissionsFromToken(String token) {
        try {
            Claims claims = parseAndVerifyToken(token);
            @SuppressWarnings("unchecked")
            Set<String> permissions = (Set<String>) claims.get("permissions");
            return permissions != null ? permissions : Set.of();
        } catch (Exception e) {
            return Set.of();
        }
    }

    @Override
    public String extractTokenId(String token) {
        try {
            Claims claims = parseAndVerifyToken(token);
            return claims.getId();
        } catch (Exception e) {
            throw new SecurityException("Invalid token: " + e.getMessage());
        }
    }

    @Override
    public UUID extractUserId(String token) {
        try {
            Claims claims = parseAndVerifyToken(token);
            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            throw new SecurityException("Invalid token: " + e.getMessage());
        }
    }

    private Claims parseAndVerifyToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
