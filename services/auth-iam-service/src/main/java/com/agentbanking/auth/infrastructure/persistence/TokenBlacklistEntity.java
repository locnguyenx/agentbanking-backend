package com.agentbanking.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for TokenBlacklist table
 */
@Entity
@Table(name = "token_blacklist")
public class TokenBlacklistEntity {

    @Id
    @Column(name = "blacklist_id")
    private UUID blacklistId;

    @Column(name = "token_jti", unique = true, nullable = false, length = 100)
    private String tokenJti;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_by", length = 100)
    private String revokedBy;

    @Column(name = "reason", length = 255)
    private String reason;

    // Getters and setters
    public UUID getBlacklistId() { return blacklistId; }
    public void setBlacklistId(UUID blacklistId) { this.blacklistId = blacklistId; }
    public String getTokenJti() { return tokenJti; }
    public void setTokenJti(String tokenJti) { this.tokenJti = tokenJti; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}