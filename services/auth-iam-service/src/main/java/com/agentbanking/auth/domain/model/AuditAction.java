package com.agentbanking.auth.domain.model;

/**
 * Enumeration of audit action types.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    AUTHENTICATE_SUCCESS,
    AUTHENTICATE_FAILURE,
    AUTHORIZATION_GRANT,
    AUTHORIZATION_DENY,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    PASSWORD_RESET,
    TOKEN_ISSUED,
    TOKEN_REFRESHED,
    TOKEN_REVOKED,
    ROLE_ASSIGNED,
    ROLE_REMOVED,
    PERMISSION_ASSIGNED,
    PERMISSION_REMOVED
}