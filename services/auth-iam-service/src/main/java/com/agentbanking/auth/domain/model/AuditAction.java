package com.agentbanking.auth.domain.model;

/**
 * Enumeration of audit action types.
 */
public enum AuditAction {
    // User management
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    USER_LOCKED,
    USER_UNLOCKED,
    USER_PASSWORD_RESET,
    USER_PASSWORD_CHANGED,
    
    // Authentication
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    TOKEN_ISSUED,
    TOKEN_REFRESHED,
    TOKEN_REVOKED,
    
    // Role/Permission
    ROLE_CREATED,
    ROLE_UPDATED,
    ROLE_DELETED,
    ROLE_ASSIGNED,
    ROLE_REMOVED,
    PERMISSION_ASSIGNED,
    PERMISSION_REMOVED,
    
    // Legacy (for backward compatibility)
    CREATE,
    UPDATE,
    DELETE,
    AUTHENTICATE_SUCCESS,
    AUTHENTICATE_FAILURE,
    AUTHORIZATION_GRANT,
    AUTHORIZATION_DENY,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    PASSWORD_RESET
}