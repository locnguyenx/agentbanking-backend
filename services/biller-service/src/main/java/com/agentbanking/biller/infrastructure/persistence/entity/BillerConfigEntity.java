package com.agentbanking.biller.infrastructure.persistence.entity;

import com.agentbanking.biller.domain.model.BillerType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "biller_config")
public class BillerConfigEntity {
    @Id
    private UUID billerId;
    
    @Column(name = "biller_code")
    private String billerCode;
    
    @Column(name = "biller_name")
    private String billerName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "biller_type")
    private BillerType billerType;
    
    @Column(name = "api_endpoint")
    private String apiEndpoint;
    
    @Column(name = "is_active")
    private boolean active;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getBillerId() { return billerId; }
    public void setBillerId(UUID billerId) { this.billerId = billerId; }
    public String getBillerCode() { return billerCode; }
    public void setBillerCode(String billerCode) { this.billerCode = billerCode; }
    public String getBillerName() { return billerName; }
    public void setBillerName(String billerName) { this.billerName = billerName; }
    public BillerType getBillerType() { return billerType; }
    public void setBillerType(BillerType billerType) { this.billerType = billerType; }
    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}