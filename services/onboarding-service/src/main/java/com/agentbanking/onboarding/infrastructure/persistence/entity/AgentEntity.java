package com.agentbanking.onboarding.infrastructure.persistence.entity;

import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.domain.model.AgentTier;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent")
public class AgentEntity {

    @Id
    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "agent_code", unique = true, nullable = false, length = 20)
    private String agentCode;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private AgentTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentStatus status;

    @Column(name = "merchant_gps_lat", nullable = false, precision = 9, scale = 6)
    private java.math.BigDecimal merchantGpsLat;

    @Column(name = "merchant_gps_lng", nullable = false, precision = 9, scale = 6)
    private java.math.BigDecimal merchantGpsLng;

    @Column(name = "mykad_number", nullable = false, length = 255)
    private String mykadNumber;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public AgentTier getTier() { return tier; }
    public void setTier(AgentTier tier) { this.tier = tier; }
    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = status; }
    public java.math.BigDecimal getMerchantGpsLat() { return merchantGpsLat; }
    public void setMerchantGpsLat(java.math.BigDecimal merchantGpsLat) { this.merchantGpsLat = merchantGpsLat; }
    public java.math.BigDecimal getMerchantGpsLng() { return merchantGpsLng; }
    public void setMerchantGpsLng(java.math.BigDecimal merchantGpsLng) { this.merchantGpsLng = merchantGpsLng; }
    public String getMykadNumber() { return mykadNumber; }
    public void setMykadNumber(String mykadNumber) { this.mykadNumber = mykadNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
