package com.agentbanking.onboarding.infrastructure.persistence.entity;

import com.agentbanking.onboarding.domain.model.OnboardingDecision;
import com.agentbanking.onboarding.domain.model.OnboardingDecisionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for onboarding decisions
 */
@Entity
@Table(name = "onboarding_decision")
public class OnboardingDecisionEntity {
    @Id
    private UUID decisionId;
    
    @Column(name = "onboarding_id")
    private UUID onboardingId;
    
    @Column(name = "decision_type")
    private String decisionType;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "reviewer_id")
    private String reviewerId;
    
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public UUID getDecisionId() { return decisionId; }
    public void setDecisionId(UUID decisionId) { this.decisionId = decisionId; }
    public UUID getOnboardingId() { return onboardingId; }
    public void setOnboardingId(UUID onboardingId) { this.onboardingId = onboardingId; }
    public String getDecisionType() { return decisionType; }
    public void setDecisionType(String decisionType) { this.decisionType = decisionType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }

    /**
     * Converts this entity to a domain record
     */
    public OnboardingDecision toRecord() {
        return new OnboardingDecision(
            decisionId,
            onboardingId,
            OnboardingDecisionType.valueOf(decisionType),
            reason,
            reviewerId,
            decidedAt
        );
    }

    /**
     * Creates an entity from a domain record
     */
    public static OnboardingDecisionEntity fromRecord(OnboardingDecision record) {
        OnboardingDecisionEntity entity = new OnboardingDecisionEntity();
        entity.setDecisionId(record.decisionId());
        entity.setOnboardingId(record.onboardingId());
        entity.setDecisionType(record.decisionType().toString());
        entity.setReason(record.reason());
        entity.setReviewerId(record.reviewerId());
        entity.setDecidedAt(record.decidedAt());
        return entity;
    }
}