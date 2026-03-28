package com.agentbanking.onboarding.infrastructure.persistence.entity;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for agent onboarding records
 */
@Entity
@Table(name = "agent_onboarding")
public class AgentOnboardingEntity {
    @Id
    private UUID onboardingId;
    
    @Column(name = "mykad_number")
    private String mykadNumber;
    
    @Column(name = "extracted_name")
    private String extractedName;
    
    @Column(name = "ssm_business_name")
    private String ssmBusinessName;
    
    @Column(name = "ssm_owner_name")
    private String ssmOwnerName;
    
    @Column(name = "agent_tier")
    private String agentTier;
    
    @Column(name = "ocr_name_match")
    private boolean ocrNameMatch;
    
    @Column(name = "ssm_active")
    private boolean ssmActive;
    
    @Column(name = "ssm_owner_match")
    private boolean ssmOwnerMatch;
    
    @Column(name = "aml_clean")
    private boolean amlClean;
    
    @Column(name = "gps_low_risk")
    private boolean gpsLowRisk;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getOnboardingId() { return onboardingId; }
    public void setOnboardingId(UUID onboardingId) { this.onboardingId = onboardingId; }
    public String getMykadNumber() { return mykadNumber; }
    public void setMykadNumber(String mykadNumber) { this.mykadNumber = mykadNumber; }
    public String getExtractedName() { return extractedName; }
    public void setExtractedName(String extractedName) { this.extractedName = extractedName; }
    public String getSsmBusinessName() { return ssmBusinessName; }
    public void setSsmBusinessName(String ssmBusinessName) { this.ssmBusinessName = ssmBusinessName; }
    public String getSsmOwnerName() { return ssmOwnerName; }
    public void setSsmOwnerName(String ssmOwnerName) { this.ssmOwnerName = ssmOwnerName; }
    public String getAgentTier() { return agentTier; }
    public void setAgentTier(String agentTier) { this.agentTier = agentTier; }
    public boolean isOcrNameMatch() { return ocrNameMatch; }
    public void setOcrNameMatch(boolean ocrNameMatch) { this.ocrNameMatch = ocrNameMatch; }
    public boolean isSsmActive() { return ssmActive; }
    public void setSsmActive(boolean ssmActive) { this.ssmActive = ssmActive; }
    public boolean isSsmOwnerMatch() { return ssmOwnerMatch; }
    public void setSsmOwnerMatch(boolean ssmOwnerMatch) { this.ssmOwnerMatch = ssmOwnerMatch; }
    public boolean isAmlClean() { return amlClean; }
    public void setAmlClean(boolean amlClean) { this.amlClean = amlClean; }
    public boolean isGpsLowRisk() { return gpsLowRisk; }
    public void setGpsLowRisk(boolean gpsLowRisk) { this.gpsLowRisk = gpsLowRisk; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Converts this entity to a domain record
     */
    public AgentOnboardingRecord toRecord() {
        return new AgentOnboardingRecord(
            onboardingId,
            mykadNumber,
            extractedName,
            ssmBusinessName,
            ssmOwnerName,
            agentTier,
            ocrNameMatch,
            ssmActive,
            ssmOwnerMatch,
            amlClean,
            gpsLowRisk,
            createdAt,
            updatedAt
        );
    }

    /**
     * Creates an entity from a domain record
     */
    public static AgentOnboardingEntity fromRecord(AgentOnboardingRecord record) {
        AgentOnboardingEntity entity = new AgentOnboardingEntity();
        entity.setOnboardingId(record.onboardingId());
        entity.setMykadNumber(record.mykadNumber());
        entity.setExtractedName(record.extractedName());
        entity.setSsmBusinessName(record.ssmBusinessName());
        entity.setSsmOwnerName(record.ssmOwnerName());
        entity.setAgentTier(record.agentTier());
        entity.setOcrNameMatch(record.ocrNameMatch());
        entity.setSsmActive(record.ssmActive());
        entity.setSsmOwnerMatch(record.ssmOwnerMatch());
        entity.setAmlClean(record.amlClean());
        entity.setGpsLowRisk(record.gpsLowRisk());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }
}