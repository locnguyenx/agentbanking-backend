package com.agentbanking.onboarding.infrastructure.persistence.entity;

import com.agentbanking.onboarding.domain.model.AmlStatus;
import com.agentbanking.onboarding.domain.model.BiometricResult;
import com.agentbanking.onboarding.domain.model.KycStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_verification")
public class KycVerificationEntity {
    @Id
    private UUID verificationId;
    
    @Column(name = "mykad_number")
    private String mykadNumber;
    
    @Column(name = "full_name")
    private String fullName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "age")
    private Integer age;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "aml_status")
    private AmlStatus amlStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "biometric_match")
    private BiometricResult biometricMatch;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private KycStatus verificationStatus;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "reviewed_by")
    private String reviewedBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getVerificationId() { return verificationId; }
    public void setVerificationId(UUID verificationId) { this.verificationId = verificationId; }
    public String getMykadNumber() { return mykadNumber; }
    public void setMykadNumber(String mykadNumber) { this.mykadNumber = mykadNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public AmlStatus getAmlStatus() { return amlStatus; }
    public void setAmlStatus(AmlStatus amlStatus) { this.amlStatus = amlStatus; }
    public BiometricResult getBiometricMatch() { return biometricMatch; }
    public void setBiometricMatch(BiometricResult biometricMatch) { this.biometricMatch = biometricMatch; }
    public KycStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(KycStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}