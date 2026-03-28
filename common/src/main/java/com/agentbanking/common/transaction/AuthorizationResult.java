package com.agentbanking.common.transaction;

/**
 * Result of an authorization request
 */
public record AuthorizationResult(boolean approved, String referenceNumber, String declineCode) {
    public boolean isApproved() {
        return approved;
    }
    
    public String getReferenceNumber() {
        return referenceNumber;
    }
    
    public String getDeclineCode() {
        return declineCode;
    }
}
